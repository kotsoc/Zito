package com.konstantinos.zito.model;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItem {
    
    private String menuItemId;         // original menu item reference
    private String name;                // snapshot of name at time of order
    private int quantity;              //total = price * quantity

    private String note;              // ✅ free-text user note
    private List<OrderItemModifier> itemModifiers = new java.util.ArrayList<>();

    //Pricing snapshot (Stored for Audit/UI) - Financial
    private BigDecimal unitPriceNet;       // snapshot of price at time of order
    private BigDecimal unitTax;
    private BigDecimal unitPriceGross;       // snapshot of price at time of order
    private BigDecimal itemDiscount;    // 0-100
    

    // Calculated Totals
    private BigDecimal discountTotal;      //Calculated at checkout
    private BigDecimal modifierTotal;       //calculated at checkout

    private TaxEntry taxEntry;        // tax rate: decimal
    private BigDecimal netTotal;
    private BigDecimal grossTotal;        
}
