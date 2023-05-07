package com.example.zito.model;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    String jwtToken;
    String username;
    Collection<String> roles;
}
