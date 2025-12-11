package com.example.chess.mapper;

import com.example.chess.model.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {
    @Select("SELECT id, username, password, avatar FROM users WHERE username = #{userName} LIMIT 1")
    User getByUsername(@Param("userName") String userName);

    @Insert("INSERT INTO users (username, password, avatar) VALUES (#{username}, #{password}, #{avatar})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);
}
