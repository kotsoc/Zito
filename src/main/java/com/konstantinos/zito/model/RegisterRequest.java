package com.konstantinos.zito.model;

import lombok.Data;

@Data
public class RegisterRequest {
    String username;
    String password;
    String phoneNumber;
}
