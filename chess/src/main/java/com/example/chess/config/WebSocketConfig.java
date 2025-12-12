package com.example.chess.config;

import com.example.chess.websocket.TvWebSocketHandler;
import com.example.chess.service.ChessAiService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChessAiService chessAiService;

    public WebSocketConfig(ChessAiService chessAiService) {
        this.chessAiService = chessAiService;
    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new TvWebSocketHandler(chessAiService), "/ws/tv")
                .setAllowedOriginPatterns(
                        "*",
                        "null",
                        "file://*",
                        "http://*",
                        "https://*",
                        "http://10.0.2.2:*",
                        "http://localhost:*",
                        "http://192.168.*:*"
                );
    }
}
