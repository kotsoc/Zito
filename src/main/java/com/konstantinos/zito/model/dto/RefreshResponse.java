package com.konstantinos.zito.model.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RefreshResponse {
    String accessToken;
    UUID userId;
}
