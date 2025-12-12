package com.example.chess.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChessAiService {
    @Autowired(required = false)
    private ChatModel chatModel;

    public String evaluateUserMoveWithHeuristic(String position, String move, String heuristicJson) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是中国象棋残局助手与规则裁判。\n");
        sb.append("走法评价按 JSON 输出：{\"win_rate\": <0-100>, \"trend\": \"妙手|缓手|败着\"}\n");
        sb.append("仅输出严格 JSON，不要添加任何额外文本或代码块围栏。\n");
        sb.append("棋盘坐标：9×10，原点左上；x=1..9 向右增，y=1..10 向下增。\n");
        sb.append("局面 JSON：{\"pieces\":[{\"type\":\"rook|cannon|horse|elephant|advisor|king|pawn\",\"side\":\"red|black\",\"x\":1..9,\"y\":1..10}],\"turn\":\"red|black\"}\n");
        sb.append("类型对应：king=将/帅，advisor=士，elephant=象，horse=马，rook=车，cannon=炮，pawn=兵/卒。\n");
        sb.append("参考本地启发：").append(heuristicJson == null ? "" : heuristicJson).append("\n");
        sb.append("局面：").append(position == null ? "" : position).append("\n");
        sb.append("用户走法：").append(move == null ? "" : move).append("\n");
        if (chatModel != null) {
            try {
                return chatModel.call(sb.toString());
            } catch (Exception e) {
                return "{\"error\":\"chatmodel_call_failed\"}";
            }
        }
        return "{\"error\":\"chatmodel_unavailable\"}";
    }

    public String adviseAiMoveQuick(String position, String side, String hint) {
        if (chatModel == null) {
            return "{\"error\":\"chatmodel_unavailable\"}";
        }
        java.util.concurrent.CompletableFuture<String> fut = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("你是中国象棋残局助手与规则裁判。\n");
            sb.append("AI 建议按 JSON 输出：{\"ai_move\": \"<着法>\", \"advice\": \"<建议>\", \"plan\": [\"要点1\",\"要点2\"], \"win_rate\": <0-100>}\n");
            sb.append("尽量在1秒内给出简洁建议（50字内），不要仅重复着法或坐标，需说明目的与注意要点。\n");
            sb.append("仅输出严格 JSON，不要添加任何额外文本或代码块围栏；确保包含 ai_move、advice、win_rate，且 plan 至少两项。\n");
            sb.append("棋盘坐标：9×10，原点左上；x=1..9 向右增，y=1..10 向下增。\n");
            sb.append("局面 JSON：{\"pieces\":[{\"type\":\"rook|cannon|horse|elephant|advisor|king|pawn\",\"side\":\"red|black\",\"x\":1..9,\"y\":1..10}],\"turn\":\"red|black\"}\n");
            sb.append("类型对应：king=将/帅，advisor=士，elephant=象，horse=马，rook=车，cannon=炮，pawn=兵/卒。\n");
            sb.append("若输出坐标着法，采用 \"x1,y1→x2,y2\" 格式，并确保合法。\n");
            if (hint != null && !hint.isEmpty()) {
                sb.append("本地快速建议：").append(hint).append("\n");
            }
            sb.append("局面：").append(position == null ? "" : position).append("\n");
            sb.append("行棋方：").append(side == null ? "red" : side).append("\n");
            return chatModel.call(sb.toString());
        });
        try {
            return fut.get(4000, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception timeout) {
            String adv = (hint != null && !hint.isEmpty()) ? hint : "稳住关键点位，形成先手";
            if (adv.length() > 50) adv = adv.substring(0,50);
            return "{\"ai_move\":\"\",\"advice\":\"" + adv + "\",\"plan\":[],\"win_rate\":50,\"status\":\"fallback\"}";
        }
    }
}
