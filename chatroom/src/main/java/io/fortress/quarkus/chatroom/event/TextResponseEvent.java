package io.fortress.quarkus.chatroom.event;

public record TextResponseEvent(
        String message
) implements IEvent {

}
