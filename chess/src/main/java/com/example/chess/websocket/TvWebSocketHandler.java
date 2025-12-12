package com.example.chess.websocket;

import com.example.chess.service.ChessAiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TvWebSocketHandler extends TextWebSocketHandler {
    private final Set<WebSocketSession> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final ChessAiService chessAiService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(4);

    public TvWebSocketHandler(ChessAiService chessAiService) {
        this.chessAiService = chessAiService;
    }

    private JsonNode normalizeAgentJson(JsonNode node) {
        try {
            if (node == null) return null;
            if (node.isTextual()) {
                JsonNode parsed = tryParseJsonText(node.asText());
                if (parsed != null && parsed.isObject()) return parsed;
            }
            if (node.has("content")) {
                String inner = node.path("content").asText("");
                if (inner != null && !inner.isEmpty()) {
                    JsonNode parsed = tryParseJsonText(inner);
                    if (parsed != null && parsed.isObject()) return parsed;
                }
            }
            if (node.has("message") && node.path("message").has("content")) {
                String inner = node.path("message").path("content").asText("");
                if (inner != null && !inner.isEmpty()) {
                    JsonNode parsed = tryParseJsonText(inner);
                    if (parsed != null && parsed.isObject()) return parsed;
                }
            }
            if (node.has("choices") && node.path("choices").isArray() && node.path("choices").size() > 0) {
                JsonNode first = node.path("choices").get(0);
                if (first != null && first.has("message") && first.path("message").has("content")) {
                    String inner = first.path("message").path("content").asText("");
                    if (inner != null && !inner.isEmpty()) {
                        JsonNode parsed = tryParseJsonText(inner);
                        if (parsed != null && parsed.isObject()) return parsed;
                    }
                }
            }
        } catch (Exception ignore) {}
        return node;
    }

    private JsonNode tryParseJsonText(String text) {
        try { return mapper.readTree(text); } catch (Exception e) { }
        if (text == null) return null;
        String s = text.trim();
        if (s.startsWith("```") && s.endsWith("```") ) {
            s = s.replaceAll("^```[a-zA-Z0-9_-]*\\n?", "");
            s = s.replaceAll("\\n?```$", "");
        }
        int start = s.indexOf('{');
        if (start < 0) return null;
        int depth = 0; int end = -1;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) { end = i; break; } }
        }
        if (end > start) {
            String sub = s.substring(start, end + 1);
            try { return mapper.readTree(sub); } catch (Exception ignore) {}
        }
        return null;
    }

    private String deriveAiMoveFromText(String text) {
        if (text == null) return "";
        String t = text.replaceAll("\n", " ").trim();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+)\\s*[,，]\\s*(\\d+)\\s*→\\s*(\\d+)\\s*[,，]\\s*(\\d+)");
        java.util.regex.Matcher m = p.matcher(t);
        if (m.find()) {
            return m.group(1) + "," + m.group(2) + "→" + m.group(3) + "," + m.group(4);
        }
        return "";
    }

    private double computeRedWinRateFromPosition(String positionJson) {
        try {
            JsonNode node = mapper.readTree(positionJson);
            JsonNode pieces = node.path("pieces");
            int score = 0;
            if (pieces.isArray()) {
                for (JsonNode p : pieces) {
                    String type = p.path("type").asText("");
                    String side = p.path("side").asText("");
                    int val;
                    switch (type) {
                        case "king": val = 10000; break;
                        case "rook": val = 90; break;
                        case "cannon": val = 45; break;
                        case "horse": val = 40; break;
                        case "elephant": val = 20; break;
                        case "advisor": val = 20; break;
                        case "pawn": val = 10; break;
                        default: val = 0; break;
                    }
                    if ("black".equals(side)) score += val; else if ("red".equals(side)) score -= val;
                }
            }
            double K = 50.0;
            double wr = 100.0 / (1.0 + Math.exp(score / K));
            if (wr < 5.0) wr = 5.0; if (wr > 95.0) wr = 95.0;
            return wr;
        } catch (Exception e) {
            return -1.0;
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        try {
            JsonNode root = mapper.readTree(payload);
            String type = root.path("type").asText();
            ObjectNode response = mapper.createObjectNode();
            response.put("type", type);
            switch (type) {
                case "round_summary": {
                    String positionCurrent = root.path("position").asText("");
                    String positionPlayer = root.path("position_player").asText("");
                    String playerMove = root.path("player_move").asText("");
                    JsonNode heuristicNode = root.path("heuristic");
                    String heuristicJson = heuristicNode.isMissingNode() ? null : heuristicNode.toString();
                    String hint = root.path("hint").asText("");
                    ObjectNode pending = mapper.createObjectNode();
                    pending.put("type", "round_summary");
                    pending.put("status", "pending");
                    session.sendMessage(new TextMessage(pending.toString().getBytes(StandardCharsets.UTF_8)));
                    pool.submit(() -> {
                        try {
                            String evalOutput = chessAiService.evaluateUserMoveWithHeuristic(positionPlayer, playerMove, heuristicJson);
                            String adviseOutput = chessAiService.adviseAiMoveQuick(positionCurrent, "red", hint);
                            ObjectNode finalResp = mapper.createObjectNode();
                            finalResp.put("type", "round_summary");
                            finalResp.put("status", "final");
                            try {
                                JsonNode evalJson = mapper.readTree(evalOutput);
                                evalJson = normalizeAgentJson(evalJson);
                                if (evalJson.has("win_rate")) finalResp.set("win_rate", evalJson.get("win_rate"));
                                if (heuristicNode != null && heuristicNode.isObject()) {
                                    double before = heuristicNode.path("score_before").asDouble(0);
                                    double after = heuristicNode.path("score_after").asDouble(0);
                                    double delta = after - before;
                                    boolean isCheck = heuristicNode.path("tactical").path("is_check_on_opponent").asBoolean(false);
                                    int capturedVal = heuristicNode.path("tactical").path("captured_value").asInt(0);
                                    String fallbackTrend = (delta <= -6) ? "妙手" : (delta >= 6 ? "败着" : "缓手");
                                    if (isCheck || capturedVal >= 40) {
                                        if (delta < 10) fallbackTrend = "妙手";
                                    }
                                    if (evalJson.has("trend")) {
                                        String agentTrend = evalJson.path("trend").asText("");
                                        boolean strongImprove = delta <= -8;
                                        boolean strongWorsen = delta >= 8;
                                        if (strongImprove && !"妙手".equals(agentTrend)) finalResp.put("trend", "妙手");
                                        else if (strongWorsen && !"败着".equals(agentTrend)) finalResp.put("trend", "败着");
                                        else finalResp.put("trend", agentTrend);
                                    } else {
                                        finalResp.put("trend", fallbackTrend);
                                    }
                                    double K = 50.0;
                                    double wrHeuristic = 100.0 / (1.0 + Math.exp(after / K));
                                    wrHeuristic = Math.max(5.0, Math.min(95.0, wrHeuristic));
                                    if (!finalResp.has("win_rate")) {
                                        finalResp.put("win_rate", Math.round(wrHeuristic));
                                    } else {
                                        double wrAgent = finalResp.path("win_rate").asDouble(50.0);
                                        if (Math.abs(wrAgent - wrHeuristic) >= 10.0) {
                                            finalResp.put("win_rate", Math.round(wrHeuristic));
                                        }
                                    }
                                } else if (evalJson.has("trend")) {
                                    finalResp.set("trend", evalJson.get("trend"));
                                }
                            } catch (Exception ignore1) { }
                            try {
                                JsonNode adviseJson = mapper.readTree(adviseOutput);
                                adviseJson = normalizeAgentJson(adviseJson);
                                String aiMoveText = adviseJson.path("ai_move").asText("");
                                if (adviseJson.has("ai_move")) finalResp.set("ai_move", adviseJson.get("ai_move"));
                                String adviceText = adviseJson.has("advice") ? adviseJson.path("advice").asText("") : "";
                                if (adviceText == null) adviceText = "";
                                // 过滤仅重复着法或坐标的建议
                                String normAdvice = adviceText.replaceAll("\\s+", "");
                                String normMove = aiMoveText == null ? "" : aiMoveText.replaceAll("\\s+", "");
                                boolean repeatsMove = !normAdvice.isEmpty() && normAdvice.equalsIgnoreCase(normMove);
                                boolean coordOnly = adviceText.matches(".*\\d+\\s*[,，]\\s*\\d+\\s*→\\s*\\d+\\s*[,，]\\s*\\d+.*");
                                if (adviceText.trim().isEmpty() || repeatsMove || coordOnly) {
                                    if (adviseJson.has("plan") && adviseJson.get("plan").isArray()) {
                                        StringBuilder sbPlan = new StringBuilder();
                                        for (JsonNode it : adviseJson.get("plan")) {
                                            String seg = it.asText("");
                                            if (!seg.isEmpty()) {
                                                if (sbPlan.length() > 0) sbPlan.append('；');
                                                sbPlan.append(seg);
                                            }
                                        }
                                        adviceText = sbPlan.toString();
                                    }
                                    if ((adviceText == null || adviceText.trim().isEmpty()) && hint != null && !hint.isEmpty()) {
                                        adviceText = hint;
                                    }
                                }
                                if ((aiMoveText == null || aiMoveText.trim().isEmpty())) {
                                    String derived = deriveAiMoveFromText(adviceText);
                                    if (!derived.isEmpty()) finalResp.put("ai_move", derived);
                                }
                                if (adviceText != null && !adviceText.isEmpty()) {
                                    if (adviceText.length() > 50) adviceText = adviceText.substring(0,50);
                                    finalResp.put("advice", adviceText);
                                }
                                if (adviseJson.has("plan")) finalResp.set("plan", adviseJson.get("plan"));
                                if (!finalResp.has("plan") || !finalResp.get("plan").isArray() || finalResp.get("plan").size() == 0) {
                                    String txt = adviceText == null ? "" : adviceText;
                                    java.util.List<String> items = new java.util.ArrayList<>();
                                    for (String part : txt.split("[；;，,。]\\s*")) {
                                        String sPart = part == null ? "" : part.trim();
                                        if (sPart.isEmpty()) continue;
                                        if (sPart.matches(".*\\d+\\s*[,，]\\s*\\d+\\s*→\\s*\\d+\\s*[,，]\\s*\\d+.*")) continue;
                                        items.add(sPart);
                                        if (items.size() >= 3) break;
                                    }
                                    if (items.isEmpty() && hint != null && !hint.isEmpty()) {
                                        for (String part : hint.split("[；;，,。]\\s*")) {
                                            String sPart = part == null ? "" : part.trim();
                                            if (sPart.isEmpty()) continue;
                                            if (sPart.matches(".*\\d+\\s*[,，]\\s*\\d+\\s*→\\s*\\d+\\s*[,，]\\s*\\d+.*")) continue;
                                            items.add(sPart);
                                            if (items.size() >= 3) break;
                                        }
                                    }
                                    if (items.isEmpty()) {
                                        items.add("巩固关键点位");
                                        items.add("形成先手或杀势");
                                    }
                                    com.fasterxml.jackson.databind.node.ArrayNode arr = mapper.createArrayNode();
                                    for (String it : items) arr.add(it);
                                    finalResp.set("plan", arr);
                                }
                                if (adviseJson.has("win_rate") && !finalResp.has("win_rate")) finalResp.set("win_rate", adviseJson.get("win_rate"));
                            } catch (Exception ignore2) { }
                            double wrOverall = computeRedWinRateFromPosition(positionCurrent);
                            if (wrOverall > 0 && !finalResp.has("win_rate")) {
                                finalResp.put("win_rate", Math.round(wrOverall));
                            }
                            session.sendMessage(new TextMessage(finalResp.toString().getBytes(StandardCharsets.UTF_8)));
                        } catch (Exception ex) {
                            ObjectNode err = mapper.createObjectNode();
                            err.put("type", "error");
                            err.put("message", "round_summary_failed");
                            try { session.sendMessage(new TextMessage(err.toString().getBytes(StandardCharsets.UTF_8))); } catch (Exception ignored) {}
                        }
                    });
                    return;
                }
                default: {
                    response.put("error", "unknown_type");
                    response.put("message", "Unsupported type: " + type);
                }
            }
            session.sendMessage(new TextMessage(response.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            ObjectNode error = mapper.createObjectNode();
            error.put("type", "error");
            error.put("message", "Invalid payload");
            session.sendMessage(new TextMessage(error.toString().getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }
}
