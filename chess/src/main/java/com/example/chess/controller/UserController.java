package com.example.chess.controller;

import com.example.chess.dto.Result;
import com.example.chess.dto.UserDTO;
import com.example.chess.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("api/user")
@CrossOrigin(origins = "*")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/registerByDevice")
    @ResponseBody
    public Result<UserDTO> registerByDevice(@RequestParam String deviceId){
        return userService.registerByDevice(deviceId);
    }

    @PostMapping("/login")
    @ResponseBody
    public Result<UserDTO> login(@RequestParam String username, @RequestParam String password){
        return userService.login(username,password);
    }
    @GetMapping("/ping")
    @ResponseBody
    public String ping(){
        return "ok";
    }
}
    
