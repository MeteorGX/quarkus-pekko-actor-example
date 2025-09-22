package io.fortress.quarkus.protobuf;


import io.quarkus.websockets.next.WebSocketConnection;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.event.LoggingAdapter;

import java.util.Arrays;


public class ProtobufClusterSession extends AbstractActor {

    final LoggingAdapter log = context().system().log();
    final WebSocketConnection connection;

    public ProtobufClusterSession(WebSocketConnection connection) {
        this.connection = connection;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Command.Connected.class, connected -> {
                    log.info("Connected By Session: {}", connected.getSessionId());
                })
                .match(Command.Disconnect.class, disconnected -> {
                    log.info("Disconnected By Session: {}, Reason: {} - {}", connection.id(), disconnected.getCode(), disconnected.getReason());
                })
                .match(Command.Exception.class, e -> {
                    log.error(e.getMessage());
                })
                .match(Command.BytesMessage.class, bytes -> {
                    if (log.isDebugEnabled())
                        log.debug("Bytes Message Request: {}", Arrays.toString(bytes.toByteArray()));

                })
                .match(Command.TextMessage.class, text -> {
                    if (log.isDebugEnabled())
                        log.debug("Text Message Request: {}", text);

                })
                .build();
    }
}
