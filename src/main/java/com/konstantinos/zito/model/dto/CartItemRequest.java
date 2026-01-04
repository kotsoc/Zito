package com.konstantinos.zito.model.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CartItemRequest {
    
    @NotBlank
    private String menuItemId;

    @Min(1)
    private int quantity;

    private String note;

    private List<String> modifiers;
}
