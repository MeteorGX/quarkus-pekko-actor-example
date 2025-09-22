package io.fortress.quarkus.protobuf;


import com.google.protobuf.ByteString;
import io.fortress.quarkus.pekko.actor.extension.ActorContainer;
import io.quarkus.websockets.next.*;
import jakarta.inject.Inject;
import org.apache.pekko.actor.ActorRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket(path = "/protobuf")
public class ProtobufClusterWebSocket {


    /**
     * 日志对象
     */
    final static Logger logger = LoggerFactory.getLogger(ProtobufClusterWebSocket.class);


    /**
     * 在线会话
     */
    final static Map<String, ActorRef> sessions = new ConcurrentHashMap<>();


    @Inject
    ActorContainer actors;


    /**
     * 会话连接
     *
     * @param session WebSocketConnection
     */
    @OnOpen
    public void connected(WebSocketConnection session) {
        logger.info("Connected to websocket: {}", session.id());
        ActorRef actor = actors.injectOf(session.id(), ProtobufClusterSession.class, () -> new ProtobufClusterSession(session));
        Command.Connected connected = Command
                .Connected
                .newBuilder()
                .setSessionId(session.id())
                .build();
        actor.tell(connected, ActorRef.noSender());
        sessions.put(session.id(), actor);
    }


    /**
     * 会话断开
     *
     * @param session WebSocketConnection
     * @param reason  CloseReason
     */
    @OnClose
    public void disconnect(WebSocketConnection session, CloseReason reason) {
        logger.info("Disconnected from websocket: {}, Reason: {}", session.id(), reason);
        ActorRef actor = sessions.remove(session.id());
        if (!Objects.isNull(actor)) {
            Command.Disconnect disconnect = Command
                    .Disconnect
                    .newBuilder()
                    .setCode(reason.getCode())
                    .setReason(reason.getMessage() == null ? "" : reason.getMessage())
                    .build();
            actor.tell(disconnect, ActorRef.noSender());
        }
        actors.remove(session.id());
    }


    /**
     * 会话异常
     */
    @OnError
    public void error(WebSocketConnection session, Exception e) {
        logger.error("Error from websocket: {}", session.id(), e);
        ActorRef actor = sessions.get(session.id());
        if (!Objects.isNull(actor)) {
            Command.Exception exception = Command
                    .Exception
                    .newBuilder()
                    .setMessage(e.getMessage() == null ? "" : e.getMessage())
                    .build();
            actor.tell(exception, ActorRef.noSender());
        }
    }

    /**
     * 文本消息传递
     */
    @OnTextMessage
    public void textMessage(WebSocketConnection session, String message) {
        logger.debug("TextMessage from websocket: {}", session.id() + ": " + message);
        ActorRef actor = sessions.get(session.id());
        if (!Objects.isNull(actor)) {
            Command.TextMessage textMessage = Command.TextMessage.newBuilder().setMessage(message).build();
            actor.tell(textMessage, ActorRef.noSender());
        }
    }


    /**
     * 二进制消息传递
     */
    @OnBinaryMessage
    public void binaryMessage(WebSocketConnection session, byte[] message) {
        logger.debug("BinaryMessage from websocket: {}", session.id() + ": " + Arrays.toString(message));
        ActorRef actor = sessions.get(session.id());
        if (!Objects.isNull(actor)) {
            Command.BytesMessage bytesMessage = Command.BytesMessage.newBuilder().setMessage(ByteString.copyFrom(message)).build();
            actor.tell(bytesMessage, ActorRef.noSender());
        }
    }


}

