package com.example.chess.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChessAiService {
    @Autowired(required = false)
    private ChatModel chatModel;

    public String adviseRoundSummary(String positionCurrent, String positionPlayer, String playerMove, String heuristicJson, String side, String hint) {
        if (chatModel == null) {
            return "{\"error\":\"chatmodel_unavailable\"}";
        }
        java.util.concurrent.CompletableFuture<String> fut = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append("你是中国象棋残局助手与规则裁判。\n");
            sb.append("输出一个合并 JSON：{\"win_rate\":<0-100>,\"trend\":\"妙手|缓手|败着\",\"ai_move\":\"<着法>\",\"advice\":\"<建议>\",\"plan\":[\"要点1\",\"要点2\"]}\n");
            sb.append("仅输出严格 JSON，不要添加任何额外文本或代码块围栏；确保包含 ai_move、advice、win_rate，且 plan 至少两项。\n");
            sb.append("advice 不超过50字；plan 至少两项、每项不超过12字、最多3项。\n");
            sb.append("棋盘坐标：9×10，原点左上；x=1..9 向右增，y=1..10 向下增。\n");
            sb.append("局面 JSON：{\"pieces\":[{\"type\":\"rook|cannon|horse|elephant|advisor|king|pawn\",\"side\":\"red|black\",\"x\":1..9,\"y\":1..10}],\"turn\":\"red|black\"}\n");
            sb.append("类型对应：king=将/帅，advisor=士，elephant=象，horse=马，rook=车，cannon=炮，pawn=兵/卒。\n");
//            sb.append("参考本地启发：").append(heuristicJson == null ? "" : heuristicJson).append("\n");
            sb.append("玩家局面：").append(positionPlayer == null ? "" : positionPlayer).append("\n");
            sb.append("玩家走法：").append(playerMove == null ? "" : playerMove).append("\n");
//            if (hint != null && !hint.isEmpty()) {
//                sb.append("本地快速建议：").append(hint).append("\n");
//            }
            sb.append("当前局面：").append(positionCurrent == null ? "" : positionCurrent).append("\n");
            sb.append("行棋方：").append(side == null ? "red" : side).append("\n");
            return chatModel.call(sb.toString());
        });
        try {
            return fut.get(4000, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception timeout) {
            String adv = (hint != null && !hint.isEmpty()) ? hint : "稳住关键点位，形成先手";
            if (adv.length() > 50) adv = adv.substring(0,50);
            return "{\"win_rate\":50,\"trend\":\"缓手\",\"ai_move\":\"\",\"advice\":\"" + adv + "\",\"plan\":[\"巩固关键点位\",\"形成先手或杀势\"]}";
        }
    }
}
