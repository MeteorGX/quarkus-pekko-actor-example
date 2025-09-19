package io.fortress.quarkus.chatroom;

import io.fortress.quarkus.chatroom.event.*;
import io.quarkus.websockets.next.WebSocketConnection;
import io.vertx.core.buffer.Buffer;
import jakarta.inject.Inject;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Cancellable;
import org.apache.pekko.actor.Scheduler;
import org.apache.pekko.event.LoggingAdapter;

import java.time.Duration;
import java.util.Objects;


public class ChatRoomSession extends AbstractActor {

    @Inject
    ChatRoomBootstrap.Bootstrap bootstrap;

    final WebSocketConnection connection;
    final Scheduler scheduler = context().system().scheduler();
    final Cancellable heartbeat;
    final LoggingAdapter log = context().system().log();


    public ChatRoomSession(WebSocketConnection connection) {
        this.connection = connection;
        this.heartbeat = scheduler.scheduleAtFixedRate(
                Duration.ofSeconds(30),
                Duration.ofSeconds(30),
                getSelf(),
                new HeartbeatEvent(),
                getContext().getDispatcher(),
                ActorRef.noSender()
        );
    }

    @Override
    public void preStart() {
        bootstrap.getAddress().tell(new JoinRoomEvent(connection.pathParam("nickname").trim()), getSelf());
    }

    @Override
    public void postStop() {
        if (!Objects.isNull(heartbeat)) heartbeat.cancel();
        bootstrap.getAddress().tell(new LeaveRoomEvent(connection.pathParam("nickname").trim()), getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(HeartbeatEvent.class, connection::isOpen, heartbeat -> connection.sendPingAndAwait(Buffer.buffer("Heartbeat")))
                .match(DisconnectedEvent.class, (event) -> log.info("Disconnected Session: {}", event))
                .match(TextRequestEvent.class, event -> bootstrap.getAddress().tell(event, getSelf()))
                .match(BinaryRequestEvent.class, event -> bootstrap.getAddress().tell(event, getSelf()))
                .match(TextResponseEvent.class, connection::isOpen, event -> connection.sendTextAndAwait(event.message()))
                .match(BinaryResponseEvent.class, connection::isOpen, event -> connection.sendBinaryAndAwait(event.message()))
                .build();
    }


}
