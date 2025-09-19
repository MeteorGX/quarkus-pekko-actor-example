package io.fortress.quarkus.chatroom;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * io.fortress.quarkus.chatroom.ChatRoomApplication.java
 * <p>
 * <a href="https://cn.quarkus.io/guides/lifecycle">Quarkus Document</a>
 */
@QuarkusMain
public class ChatRoomApplication {
    public static void main(String[] args) {
        Quarkus.run(args);
    }
}
