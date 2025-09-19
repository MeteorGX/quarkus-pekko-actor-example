package io.fortress.quarkus.chatroom;

import io.fortress.quarkus.chatroom.event.*;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.WebSocketConnection;
import io.vertx.core.buffer.Buffer;
import jakarta.inject.Inject;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Cancellable;
import org.apache.pekko.actor.Scheduler;
import org.apache.pekko.cluster.Cluster;
import org.apache.pekko.event.LoggingAdapter;

import java.time.Duration;
import java.util.Objects;

public class ChatRoomClusterSession extends AbstractActor {

    @Inject
    ChatRoomClusterBootstrap.Bootstrap bootstrap;

    final LoggingAdapter log = context().system().log();
    final WebSocketConnection connection;
    final Scheduler scheduler = context().system().scheduler();
    final Cluster cluster = Cluster.get(context().system());
    final Cancellable heartbeat;

    public ChatRoomClusterSession(WebSocketConnection connection) {
        this.connection = connection;
        this.heartbeat = scheduler.scheduleAtFixedRate(
                Duration.ofSeconds(30),
                Duration.ofSeconds(30),
                getSelf(),
                new HeartbeatEvent(),
                getContext().getDispatcher(),
                ActorRef.noSender()
        );
        cluster.registerOnMemberRemoved(() -> connection.closeAndAwait(CloseReason.INTERNAL_SERVER_ERROR));
    }

    @Override
    public void preStart() {
        bootstrap.getAddress().tell(new JoinRoomBroadcastEvent(connection.pathParam("nickname").trim()), getSelf());
    }

    @Override
    public void postStop() {
        if (!Objects.isNull(heartbeat)) heartbeat.cancel();
        bootstrap.getAddress().tell(new LeaveRoomBroadcastEvent(connection.pathParam("nickname").trim()), getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(HeartbeatEvent.class, connection::isOpen, heartbeat -> connection.sendPingAndAwait(Buffer.buffer("Heartbeat")))
                .match(DisconnectedEvent.class, (event) -> log.info("Disconnected Session: {}", event))
                .match(TextRequestEvent.class, event -> bootstrap.getAddress().tell(new TextRequestBroadcastEvent(event.nickname(), event.message()), getSelf()))
                .match(BinaryRequestEvent.class, event -> bootstrap.getAddress().tell(new BinaryRequestBroadcastEvent(event.nickname(), event.message()), getSelf()))
                .match(TextResponseEvent.class, connection::isOpen, event -> connection.sendTextAndAwait(event.message()))
                .match(BinaryResponseEvent.class, connection::isOpen, event -> connection.sendBinaryAndAwait(event.message()))
                .build();
    }
}
