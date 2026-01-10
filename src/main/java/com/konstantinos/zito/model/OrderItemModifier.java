package com.konstantinos.zito.model;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemModifier {
    private UUID optionId;
    private String name;
    private BigDecimal netPrice;
    private BigDecimal taxRate;    
    
    private BigDecimal unitNet;    //Snapshot
    private BigDecimal unitGross; // Snapshot
    private BigDecimal unitTax;


}
