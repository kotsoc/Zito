package com.konstantinos.zito.model;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {
    
    private String menuItemId;         // original menu item reference
    private String name;               // snapshot of name at time of order
    private BigDecimal unitPrice;          // snapshot of price at time of order
    private int quantity;              //total = price * quantity
    private BigDecimal discount;

    private String note;              // ✅ free-text user note
    // Optional future-proofing
    private List<String> modifiers;  // e.g. ["NO_ONIONS", "EXTRA_CHEESE"]


}
