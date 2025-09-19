package io.fortress.quarkus.chatroom.event;

public record JoinRoomBroadcastEvent(
        String nickname
) implements IEvent {
}
