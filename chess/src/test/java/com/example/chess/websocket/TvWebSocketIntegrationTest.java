package com.example.chess.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TvWebSocketIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    public void connectAndEcho() throws Exception {
        URI uri = URI.create("ws://localhost:" + port + "/ws/tv");
        CountDownLatch openLatch = new CountDownLatch(1);
        CountDownLatch messageLatch = new CountDownLatch(1);
        AtomicReference<String> received = new AtomicReference<>();

        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                openLatch.countDown();
                String payload = "{\"type\":\"user_move_eval\",\"position\":\"\",\"move\":\"炮二平五\"}";
                webSocket.sendText(payload, true);
                WebSocket.Listener.super.onOpen(webSocket);
            }

            @Override
            public java.util.concurrent.CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                received.set(data.toString());
                messageLatch.countDown();
                return null;
            }
        };

        WebSocket ws = HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(uri, listener).join();
        boolean opened = openLatch.await(3, TimeUnit.SECONDS);
        assertTrue(opened);
        boolean messaged = messageLatch.await(3, TimeUnit.SECONDS);
        assertTrue(messaged);
        String text = received.get();
        boolean containsType = text != null && text.contains("\"type\":\"user_move_eval\"");
        assertTrue(containsType);
        ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye").join();
    }
}
