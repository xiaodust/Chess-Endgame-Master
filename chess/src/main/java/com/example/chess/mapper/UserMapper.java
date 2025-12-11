package com.example.chess.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.chess.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    User getByUsername(String userName);

}
