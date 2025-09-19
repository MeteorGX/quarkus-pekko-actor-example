package io.fortress.quarkus.chatroom.event;

public record BinaryResponseEvent(
        byte[] message
) {
}
