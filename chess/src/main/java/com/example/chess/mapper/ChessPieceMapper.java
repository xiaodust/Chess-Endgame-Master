package com.example.chess.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chess.model.ChessPiece;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChessPieceMapper extends BaseMapper<ChessPiece> {
}
