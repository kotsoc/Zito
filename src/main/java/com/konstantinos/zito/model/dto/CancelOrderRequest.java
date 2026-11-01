package com.konstantinos.zito.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CancelOrderRequest {
    @NotNull @PositiveOrZero Long version;
    @NotBlank @Size(max = 500) String reason;
}
