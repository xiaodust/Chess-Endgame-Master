package com.example.chess.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chess.model.Level;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LevelMapper extends BaseMapper<Level> {
}
