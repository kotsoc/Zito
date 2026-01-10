package com.konstantinos.zito.model.dto;

import java.math.BigDecimal;
import java.util.List;

import com.konstantinos.zito.model.OrderItemModifier;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderItemResponse {
    private String menuItemId;        
    private String name;              
    private BigDecimal unitPrice;         // SnapShot at time of creation
    private int quantity;
    private BigDecimal discount;    
    private BigDecimal tax;
    private BigDecimal lineTotal;      // price * quantity - discount                 
    private List<OrderItemModifier> modifiers;
    private String note;   
}
