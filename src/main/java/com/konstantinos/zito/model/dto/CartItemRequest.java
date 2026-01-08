package com.konstantinos.zito.model.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemRequest {
    
    @NotBlank
    private String menuItemId;

    @Size(min = 1, message = "At least one item neede per CartItem")
    private int quantity;

    private String note;

    private List<String> modifiers;
}
