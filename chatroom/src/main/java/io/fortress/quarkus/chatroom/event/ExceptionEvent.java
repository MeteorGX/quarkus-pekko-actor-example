package io.fortress.quarkus.chatroom.event;

public record ExceptionEvent(
        Exception exception
) implements IEvent {
}
