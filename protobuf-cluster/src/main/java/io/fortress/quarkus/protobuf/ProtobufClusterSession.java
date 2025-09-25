package io.fortress.quarkus.protobuf;


import io.quarkus.websockets.next.WebSocketConnection;
import org.apache.pekko.actor.AbstractActor;
import org.apache.pekko.event.LoggingAdapter;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
                .match(String.class, message -> {
                    var msg = Command.TextMessage.parseFrom(message.getBytes(StandardCharsets.UTF_8));
                    getSelf().tell(msg, getSender());
                })
                .match(ByteBuffer.class, byteBuffer -> {
                    var msg = Command.BytesMessage.parseFrom(byteBuffer.array());
                    getSelf().tell(msg, getSender());
                })

                .match(Command.BytesMessage.class, bytes -> {
                    if (log.isDebugEnabled()) {
                        if (bytes.getId() == 100) {
                            log.debug("Received String Message: {}", bytes.getMessage().toStringUtf8());
                        } else {
                            log.debug("Bytes Message Request: {} - {}", bytes.getId(), Arrays.toString(bytes.getMessage().toByteArray()));
                        }
                    }
                })
                .match(Command.TextMessage.class, text -> {
                    if (log.isDebugEnabled())
                        log.debug("Text Message Request: {} - {}", text.getId(), text.getMessage());

                })
                .build();
    }
}
