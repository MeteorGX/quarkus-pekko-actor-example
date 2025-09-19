package io.fortress.quarkus.chatroom.event;

import io.quarkus.websockets.next.CloseReason;

public record DisconnectedEvent(
        CloseReason reason
) implements IEvent {
}
