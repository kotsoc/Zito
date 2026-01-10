package com.konstantinos.zito.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import com.konstantinos.zito.model.TaxEntry;

public class OrderTotals {
    private final BigDecimal grossTotal;
    private final BigDecimal netTotal;
    private final BigDecimal discountTotal;
    private final List<TaxEntry> taxTotalsByRate;

    public OrderTotals(BigDecimal grandTotal, BigDecimal subtotal,BigDecimal discountAmount,  List<TaxEntry> taxTotals) {
        this.grossTotal = grandTotal;
        this.netTotal = subtotal;
        this.discountTotal =discountAmount;

        // Sort tax rates ascending for receipt display
        this.taxTotalsByRate = taxTotals;
    }

    public BigDecimal getGrossTotal() { return grossTotal; }
    public BigDecimal getNetTotal() { return netTotal; }
    public BigDecimal getDiscountTotal() {return discountTotal;}
    public List<TaxEntry> getTaxTotalsByRate() { return taxTotalsByRate; }
}
