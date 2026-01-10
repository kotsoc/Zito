package com.konstantinos.zito.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaxEntry {
    private BigDecimal taxRate;   // e.g., 24.0
    private BigDecimal amount; // e.g., 6.97

    public TaxEntry(BigDecimal taxRate){
        this.taxRate = taxRate;
    }
}