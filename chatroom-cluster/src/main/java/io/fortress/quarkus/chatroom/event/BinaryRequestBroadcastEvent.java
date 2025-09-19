package io.fortress.quarkus.chatroom.event;

public record BinaryRequestBroadcastEvent(
        String nickname,
        byte[] message
) implements IEvent {
}
