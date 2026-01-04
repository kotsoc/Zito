package com.konstantinos.zito.model;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RefreshResponse {
    String jwtToken;
    UUID userId;
}
