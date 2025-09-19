package io.fortress.quarkus.chatroom.event;

public record TextRequestEvent(
        String nickname,
        String message
) implements IEvent {

}
