package com.example.chess.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("`level_pieces`")
public class ChessPiece {
    @TableId(value = "id", type = IdType.AUTO)
    private long id;
    private int levelId;
    private String name;
    private String type;
    private String side;
    private int x;
    private int y;
}
