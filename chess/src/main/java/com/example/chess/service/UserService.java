package com.example.chess.service;

import com.example.chess.dto.Result;
import com.example.chess.dto.UserDTO;

public interface UserService {

    Result<UserDTO> login(String userName, String password);

    Result<UserDTO> register();

    Result<UserDTO> registerByDevice(String deviceId);
}
