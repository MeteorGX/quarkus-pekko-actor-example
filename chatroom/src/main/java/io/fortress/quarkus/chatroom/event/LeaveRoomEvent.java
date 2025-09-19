package io.fortress.quarkus.chatroom.event;

public record LeaveRoomEvent(
        String nickname
) implements IEvent {
}
