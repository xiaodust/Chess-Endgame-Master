package com.example.chess.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("`levels`")
public class Level {
    @TableId(value = "id", type = IdType.AUTO)
    private int id;
    private String code;
    private String title;
    private String description;
    private int difficulty;
    private String tag;
    @TableField(exist = false)
    private List<ChessPiece> pieces;


}
