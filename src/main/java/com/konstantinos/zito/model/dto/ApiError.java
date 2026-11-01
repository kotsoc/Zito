package com.konstantinos.zito.model.dto;

import lombok.Value;

@Value
public class ApiError {
    String code;
    String traceId;
}
