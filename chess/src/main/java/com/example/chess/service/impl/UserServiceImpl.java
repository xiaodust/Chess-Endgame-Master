package com.example.chess.service.impl;

import com.example.chess.dto.Result;
import com.example.chess.dto.UserDTO;
import com.example.chess.mapper.UserMapper;
import com.example.chess.service.UserService;
import lombok.extern.slf4j.Slf4j;
import com.example.chess.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
        User user = userMapper.getByUsername(userName);
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
    public Result<UserDTO> registerByDevice(String deviceId) {
        log.info("---设备注册---");
        Result<UserDTO> result = new Result<>();
        String shortId = shortId(deviceId);
        User existing = userMapper.getByUsername(shortId);
        if (existing != null) {
            result.setCode("200");
            result.setSuccess(true);
            result.setMessage("已注册");
            result.setData(existing.TODO());
            log.info("设备已注册");
            return result;
        }
        User user = new User();
        user.setUsername(shortId);
        user.setPassword(UUID.randomUUID().toString().substring(0,12));
        userMapper.insertUser(user);
        result.setCode("200");
        result.setSuccess(true);
        result.setMessage("注册成功");
        result.setData(user.TODO());
        log.info("设备注册完成");
        return result;
    }

    private String shortId(String deviceId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(deviceId.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                String h = Integer.toHexString((b[i] & 0xFF) | 0x100).substring(1);
                sb.append(h);
            }
            return "tv-" + sb.toString();
        } catch (Exception e) {
            return ("tv-" + deviceId).replaceAll("[^a-zA-Z0-9_-]", "").substring(0, Math.min(18, Math.max(1, ("tv-" + deviceId).length())));
        }
    }
}
