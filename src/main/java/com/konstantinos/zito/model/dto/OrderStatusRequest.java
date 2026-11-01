package com.konstantinos.zito.model.dto;

import com.konstantinos.zito.model.OrderStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusRequest {
    @NotNull
    OrderStatus status;
}
