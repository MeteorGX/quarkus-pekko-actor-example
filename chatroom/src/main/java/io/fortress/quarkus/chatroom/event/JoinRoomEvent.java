package io.fortress.quarkus.chatroom.event;

public record JoinRoomEvent(
        String nickname
) implements IEvent {
}
