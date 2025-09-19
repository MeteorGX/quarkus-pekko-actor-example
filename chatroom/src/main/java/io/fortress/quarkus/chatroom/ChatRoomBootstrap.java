package io.fortress.quarkus.chatroom;

import io.fortress.quarkus.chatroom.event.*;
import io.fortress.quarkus.pekko.actor.extension.ActorContainer;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.event.LoggingAdapter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class ChatRoomBootstrap {

    public class Bootstrap {

        public String getName() {
            return name;
        }

        public ActorRef getAddress() {
            return address;
        }

        private final String name;
        private final ActorRef address;

        public Bootstrap(String name, ActorRef address) {
            this.name = name;
            this.address = address;
        }
    }


    @Startup
    @Default
    @Produces
    @ApplicationScoped
    public Bootstrap createBootstrap(ActorContainer container) {
        String name = container.system().name();
        ActorRef address = container.actorOf(name, ChatRoomDisposer.class, () -> new ChatRoomDisposer(name));
        return new Bootstrap(name, address);
    }


    public static class ChatRoomDisposer extends AbstractActor {

        final String name;
        final List<ActorRef> actors = new ArrayList<>();
        final LoggingAdapter log = context().system().log();

        public ChatRoomDisposer(String name) {
            this.name = name;
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(JoinRoomEvent.class, (event) -> {
                        String recv = "[%s] %s - join room".formatted(name, event.nickname());
                        actors.forEach(actor -> actor.tell(new TextResponseEvent(recv), ActorRef.noSender()));
                        ActorRef sender = getSender();
                        actors.add(sender);
                        log.info("Join Room: {}", sender.path());
                    })
                    .match(LeaveRoomEvent.class, (event) -> {
                        ActorRef sender = getSender();
                        actors.remove(sender);
                        log.info("Leave Room: {}", sender.path());

                        String recv = "[%s] %s - leave room".formatted(name, event.nickname());
                        actors.forEach(actor -> actor.tell(new TextResponseEvent(recv), ActorRef.noSender()));
                    })
                    .match(TextRequestEvent.class, (event) -> {
                        ActorRef sender = getSender();
                        log.info("Forward TextMessage By {}, Total: {}", sender.path(), actors.size());
                        String recv = "[%s] %s: %s".formatted(name, event.nickname(), event.message());
                        actors.forEach(actor -> actor.tell(new TextResponseEvent(recv), ActorRef.noSender()));
                    })
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
