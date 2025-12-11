package com.example.chess.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.example.chess.dto.UserDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.BeanUtils;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("`users`")
public class User {
    private  long id;
    private  String username;
    private  String password;
    private  String avatar;

    public UserDTO TODO(){
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(this,userDTO);
        return userDTO;
    }
}
