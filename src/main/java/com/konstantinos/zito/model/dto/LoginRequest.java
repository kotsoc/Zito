package com.konstantinos.zito.model.dto;

import lombok.Data;

@Data
public class LoginRequest {
    String email;
    String password;
}
