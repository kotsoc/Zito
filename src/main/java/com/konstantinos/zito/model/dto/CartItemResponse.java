package com.konstantinos.zito.model.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CartItemResponse {
    private String menuItemId;        
    private String name;              
    private BigDecimal price;         // Calculated price from backend
    private int quantity;
    private BigDecimal discount;      
    private String note;              
    private List<String> modifiers;
    private BigDecimal subtotal;      // price * quantity - discount
}
