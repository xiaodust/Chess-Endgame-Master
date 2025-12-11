package com.example.chess.controller;

import com.example.chess.dto.Result;
import com.example.chess.dto.UserDTO;
import com.example.chess.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    @ResponseBody
    public Result<UserDTO> register(){
        return  userService.register();
    }

    @PostMapping("/login")
    @ResponseBody
    public Result<UserDTO> login(@RequestParam String username, @RequestParam String password){
        return userService.login(username,password);
    }
}
