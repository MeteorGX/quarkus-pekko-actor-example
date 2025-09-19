package io.fortress.quarkus.chatroom.event;

public record LeaveRoomBroadcastEvent(
        String nickname
) implements IEvent {
}
