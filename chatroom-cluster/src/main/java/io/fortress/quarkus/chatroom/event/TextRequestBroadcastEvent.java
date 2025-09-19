package io.fortress.quarkus.chatroom.event;

public record TextRequestBroadcastEvent(
        String nickname,
        String message
) implements IEvent {

}
