package com.example.chess.service.impl;

import com.example.chess.dto.Result;
import com.example.chess.dto.UserDTO;
import com.example.chess.mapper.UserMapper;
import com.example.chess.service.UserService;
import lombok.extern.slf4j.Slf4j;
import com.example.chess.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private  UserMapper userMapper;

    @Override
    public Result<UserDTO> login(String userName, String password) {
        log.info("---开始登录---");
        Result<UserDTO> result = new Result<>();
        User user=userMapper.getByUsername(userName);
        if(user==null){
            result.setCode("601");
            result.setSuccess(false);
            result.setMessage("用户不存在");
            return result;
        }
        if(!user.getPassword().equals(password)){
            result.setCode("602");
            result.setSuccess(false);
            result.setMessage("用户名或者密码不正确");
            return  result;
        }
        result.setCode("200");
        result.setSuccess(true);
        result.setMessage("登录成功");
        result.setData(user.TODO());
        log.info("登录完成");
        return  result;
    }

    @Override
    public Result<UserDTO> register() {
        log.info("---开始注册---");
        Result <UserDTO> result = new Result<>();
        User user=new User();
        user.setUsername(UUID.randomUUID().toString().substring(0,12));
        user.setPassword(UUID.randomUUID().toString().substring(0,12));
        userMapper.insert(user);
        result.setCode("200");
        result.setSuccess(true);
        result.setMessage("注册成功");
        result.setData(user.TODO());
        log.info("注册完成");
        return  result;
    }
}
