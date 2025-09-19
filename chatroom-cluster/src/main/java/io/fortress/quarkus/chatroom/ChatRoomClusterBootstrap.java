package io.fortress.quarkus.chatroom;

import io.fortress.quarkus.chatroom.event.*;
import io.fortress.quarkus.pekko.actor.extension.ActorContainer;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.ActorSelection;
import org.apache.pekko.actor.ActorSystem;
import org.apache.pekko.cluster.Cluster;
import org.apache.pekko.cluster.ClusterEvent;
import org.apache.pekko.cluster.Member;
import org.apache.pekko.cluster.MemberStatus;
import org.apache.pekko.cluster.pubsub.DistributedPubSub;
import org.apache.pekko.cluster.pubsub.DistributedPubSubMediator;
import org.apache.pekko.event.LoggingAdapter;
import org.apache.pekko.management.javadsl.PekkoManagement;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class ChatRoomClusterBootstrap {

    public static final class Bootstrap {


        public ActorRef getAddress() {
            return address;
        }


        public Cluster getCluster() {
            return cluster;
        }

        private final ActorRef address;

        private final Cluster cluster;

        public Bootstrap(ActorRef address, Cluster cluster) {
            this.address = address;
            this.cluster = cluster;
        }
    }


    @Startup
    @Default
    @Produces
    @ApplicationScoped
    public Bootstrap createBootstrap(ActorContainer container) {
        ActorSystem system = container.system();
        Cluster cluster = Cluster.get(system);
        PekkoManagement management = PekkoManagement.get(system);
        String name = "%s-room".formatted(system.name());
        ActorRef room = container.injectOf(name, ChatRoomDisposer.class, () -> new ChatRoomDisposer(name));

        cluster.registerOnMemberUp(() -> {
            management.start();

        });

        cluster.registerOnMemberRemoved(() -> {

            management.stop();
            Quarkus.asyncExit(1);
        });


        return new Bootstrap(room, cluster);
    }


    public static class ChatRoomDisposer extends AbstractActor {

        final String name;
        final Cluster cluster = Cluster.get(context().system());
        final LoggingAdapter log = context().system().log();
        final List<ActorRef> actors = new ArrayList<>();
        final DistributedPubSub distributed = DistributedPubSub.get(context().system());

        public ChatRoomDisposer(String name) {
            this.name = name;
        }

        @Override
        public void preStart() {
            cluster.subscribe(
                    getSelf(),
                    ClusterEvent.initialStateAsEvents(),
                    ClusterEvent.MemberUp.class,
                    ClusterEvent.MemberRemoved.class,
                    ClusterEvent.ReachableMember.class,
                    ClusterEvent.UnreachableMember.class
            );

            // subscribe
            distributed.mediator().tell(
                    new DistributedPubSubMediator.Subscribe(name, getSelf()),
                    getSelf()
            );
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(ClusterEvent.MemberUp.class, memberUp -> {
                        Member active = memberUp.member();
                        log.info("Join member: {}", active.address());
                    })
                    .match(ClusterEvent.MemberRemoved.class, memberRemoved -> {
                        Member active = memberRemoved.member();
                        log.info("Leave member: {}", active.address());
                    })
                    .match(ClusterEvent.ReachableMember.class, reachableMember -> {
                        Member active = reachableMember.member();
                        log.info("Reachable member: {}", active.address());
                    })
                    .match(ClusterEvent.UnreachableMember.class, unreachableMember -> {
                        Member active = unreachableMember.member();
                        log.info("Unreachable member: {}", active.address());
                    })

                    // broadcast - join room
                    .match(JoinRoomBroadcastEvent.class, event -> {
                        ActorRef sender = getSender();
                        actors.add(sender);
                        log.info("Join Room: {}", sender.path());

                        // publish
                        distributed.mediator().tell(new DistributedPubSubMediator.Publish(
                                name,
                                new JoinRoomEvent(event.nickname())
                        ), getSelf());
                    })

                    //  broadcast - leave room
                    .match(LeaveRoomBroadcastEvent.class, event -> {
                        ActorRef sender = getSender();
                        actors.remove(sender);
                        log.info("Leave Room: {}", sender.path());
                        // publish
                        distributed.mediator().tell(new DistributedPubSubMediator.Publish(
                                name,
                                new LeaveRoomEvent(event.nickname())
                        ), getSelf());
                    })


                    // broadcast - text message
                    .match(TextRequestBroadcastEvent.class, request -> {
                        // publish
                        distributed.mediator().tell(new DistributedPubSubMediator.Publish(
                                name,
                                new TextRequestEvent(request.nickname(), request.message())
                        ), getSelf());
                    })

                    // broadcast - binary message
                    .match(BinaryRequestBroadcastEvent.class, request -> {
                        // publish
                        distributed.mediator().tell(new DistributedPubSubMediator.Publish(
                                name,
                                new BinaryRequestEvent(request.nickname(), request.message())
                        ), getSelf());
                    })


                    // event - join room
                    .match(JoinRoomEvent.class, event -> {
                        String recv = "[%s] %s - join room".formatted(name, event.nickname());
                        actors.forEach(actor -> actor.tell(new TextResponseEvent(recv), ActorRef.noSender()));
                    })

                    // event - leave room
                    .match(LeaveRoomEvent.class, event -> {
                        String recv = "[%s] %s - leave room".formatted(name, event.nickname());
                        actors.forEach(actor -> actor.tell(new TextResponseEvent(recv), ActorRef.noSender()));
                    })


                    // event - text message
                    .match(TextRequestEvent.class, (event) -> {
                        ActorRef sender = getSender();
                        log.info("Forward TextMessage By {}, Total: {}", sender.path(), actors.size());
                        String recv = "[%s] %s: %s".formatted(name, event.nickname(), event.message());
                        actors.forEach(actor -> actor.tell(new TextResponseEvent(recv), ActorRef.noSender()));
                    })

                    // event - binary message
                    .match(BinaryRequestEvent.class, (event) -> {
                        ActorRef sender = getSender();
                        log.info("Forward BinaryMessage By {}, Total: {}", sender.path(), actors.size());
                        String recv = "[%s] %s: %s".formatted(name, event.nickname(), Arrays.toString(event.message()));
                        actors.forEach(actor -> actor.tell(new BinaryResponseEvent(recv.getBytes(StandardCharsets.UTF_8)), ActorRef.noSender()));
                    })

                    .build();
        }
    }

}
