package io.fortress.quarkus.chatroom;

import io.fortress.quarkus.chatroom.event.BinaryRequestEvent;
import io.fortress.quarkus.chatroom.event.DisconnectedEvent;
import io.fortress.quarkus.chatroom.event.ExceptionEvent;
import io.fortress.quarkus.chatroom.event.TextRequestEvent;
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

@WebSocket(path = "/chatroom/{nickname}")
public class ChatRoomWebSocket {

    /**
     * 日志对象
     */
    final static Logger logger = LoggerFactory.getLogger(ChatRoomWebSocket.class);

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
        ActorRef actor = actors.injectOf(session.id(), ChatRoomSession.class, () -> new ChatRoomSession(session));
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
        if (!Objects.isNull(actor)) actor.tell(new DisconnectedEvent(reason), ActorRef.noSender());

        // remote
        actors.remove(session.id());
    }


    /**
     * 会话异常
     */
    @OnError
    public void error(WebSocketConnection session, Exception e) {
        logger.error("Error from websocket: {}", session.id(), e);
        ActorRef actor = sessions.get(session.id());
        if (!Objects.isNull(actor)) actor.tell(new ExceptionEvent(e), ActorRef.noSender());
    }

    /**
     * 文本消息传递
     */
    @OnTextMessage
    public void textMessage(WebSocketConnection session, String message) {
        logger.info("TextMessage from websocket: {}", session.id() + ": " + message);
        ActorRef actor = sessions.get(session.id());
        String nickname = session.pathParam("nickname").trim();
        if (!Objects.isNull(actor)) actor.tell(new TextRequestEvent(nickname, message), ActorRef.noSender());
    }

    /**
     * 二进制消息传递
     */
    @OnBinaryMessage
    public void binaryMessage(WebSocketConnection session, byte[] message) {
        logger.info("BinaryMessage from websocket: {}", session.id() + ": " + Arrays.toString(message));
        ActorRef actor = sessions.get(session.id());
        String nickname = session.pathParam("nickname").trim();
        if (!Objects.isNull(actor)) actor.tell(new BinaryRequestEvent(nickname, message), ActorRef.noSender());
    }


}
