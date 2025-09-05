package com.konstantinos.zito.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RefreshResponse {
    String jwtToken;
    String username;
}
