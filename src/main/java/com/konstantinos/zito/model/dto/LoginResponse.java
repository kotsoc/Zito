package com.konstantinos.zito.model.dto;

import java.util.Collection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    String accessToken;
    String username;
    Collection<String> roles;
}