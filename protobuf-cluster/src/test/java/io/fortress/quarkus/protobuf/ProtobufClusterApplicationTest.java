package io.fortress.quarkus.protobuf;


import com.google.protobuf.ByteString;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.CloseReason;
import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.wildfly.common.Assert;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

@QuarkusTest
class ProtobufClusterApplicationTest {

    final Logger log = Logger.getLogger(ProtobufClusterApplicationTest.class);

    @Test
    public void testConnected() {
        Command.Connected connected = Command
                .Connected
                .newBuilder()
                .setSessionId(UUID.randomUUID().toString())
                .build();
        Assertions.assertNotNull(connected);

        byte[] bytes = connected.toByteArray();
        Assert.assertTrue(bytes.length > 0);
        log.debugf("Command.Connected = %s", Arrays.toString(bytes));
    }

    @Test
    public void testDisconnect() {
        CloseReason closeReason = CloseReason.NORMAL;
        Command.Disconnect disconnect = Command
                .Disconnect
                .newBuilder()
                .setCode(closeReason.getCode())
                .setReason(closeReason.getMessage() == null ? "" : closeReason.getMessage())
                .build();
        Assertions.assertNotNull(disconnect);
        byte[] bytes = disconnect.toByteArray();
        Assert.assertTrue(bytes.length > 0);
        log.debugf("Command.Disconnect = %s", Arrays.toString(bytes));
    }

    @Test
    public void testException() {
        Exception exception = new RuntimeException("test exception");
        Command.Exception except = Command
                .Exception
                .newBuilder()
                .setMessage(exception.getMessage())
                .build();
        Assertions.assertNotNull(except);
        byte[] bytes = except.toByteArray();
        Assert.assertTrue(bytes.length > 0);
        log.debugf("Command.Exception = %s", Arrays.toString(bytes));
    }


    @Test
    public void testTextMessage() {
        String text = "hello world";
        Command.TextMessage message = Command
                .TextMessage
                .newBuilder()
                .setMessage(text)
                .build();
        Assertions.assertNotNull(message);
        byte[] data = message.toByteArray();
        Assert.assertTrue(data.length > 0);
        log.debugf("Command.TextMessage = %s", Arrays.toString(data));
    }


    @Test
    public void testBytesMessage() {
        byte[] bytes = "hello world".getBytes(StandardCharsets.UTF_8);
        Command.BytesMessage message = Command
                .BytesMessage
                .newBuilder()
                .setMessage(ByteString.copyFrom(bytes))
                .build();
        Assertions.assertNotNull(message);
        byte[] data = message.toByteArray();
        Assert.assertTrue(data.length > 0);
        log.debugf("Command.BytesMessage = %s", Arrays.toString(data));
    }


    @Test
    public void postmanWebSocketBytesBase64() {
        // command body
        long uid = 10001L;
        String token = DigestUtils.md5Hex("This is login token");
        byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES + tokenBytes.length);
        byteBuffer.putLong(uid);
        byteBuffer.put(tokenBytes);
        byte[] commandBody = byteBuffer.array();
        log.debugf("Command Body = %s", Arrays.toString(commandBody));

        // command header
        int cmd = 101; // login command
        Command.BytesMessage commandWrapped = Command
                .BytesMessage
                .newBuilder()
                .setId(cmd)
                .setMessage(ByteString.copyFrom(commandBody))
                .build();
        Assertions.assertNotNull(commandWrapped);
        byte[] data = commandWrapped.toByteArray();
        Assert.assertTrue(data.length > 0);
        log.debugf("Command Wrapped = %s", Arrays.toString(data));

        // convert base64
        // CGUSKAAAAAAAACcRNmYwNDIxNzY2YTkwM2U4NDAwMzI2MTk0YTlhNDJmODA=
        String postmanData = Base64.getEncoder().encodeToString(data);
        log.debugf("PostmanData = %s", postmanData);
    }

    @Test
    public void godotMessageTest(){
        Command.BytesMessage message = Command
                .BytesMessage
                .newBuilder()
                .setId(100)
                .setMessage(ByteString.copyFrom("test".getBytes(StandardCharsets.UTF_8)))
                .build();
        Assertions.assertNotNull(message);
        byte[] data = message.toByteArray();
        Assert.assertTrue(data.length > 0);
        // [8, 100, 18, 4, 116, 101, 115, 116]
        log.debugf("Godot Message = %s", Arrays.toString(data));
    }

}