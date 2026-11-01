package com.konstantinos.zito.model.dto;

import java.util.List;

import com.konstantinos.zito.model.OrderItemModifier;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequest {
    
    @NotBlank
    private String menuItemId;

    @Min(1)
    @Max(1000)
    private int quantity;

    private String note;

    private List<OrderItemModifier> modifiers = new java.util.ArrayList<>();
    @Min(0)
    @Max(100)
    private int itemDiscount;
}
