package io.fortress.quarkus.chatroom.event;

public record BinaryRequestEvent(
        String nickname,
        byte[] message
) implements IEvent {
}
