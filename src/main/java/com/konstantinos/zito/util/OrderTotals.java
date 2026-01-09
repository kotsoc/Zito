package com.konstantinos.zito.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class OrderTotals {
    private final BigDecimal grandTotal;
    private final BigDecimal subtotal;
    private final LinkedHashMap<BigDecimal, BigDecimal> taxTotalsByRate;

    public OrderTotals(BigDecimal grandTotal, BigDecimal subtotal, Map<BigDecimal, BigDecimal> taxTotals) {
        this.grandTotal = grandTotal;
        this.subtotal = subtotal;

        // Sort tax rates ascending for receipt display
        LinkedHashMap<BigDecimal, BigDecimal> sorted = new LinkedHashMap<>();
        taxTotals.entrySet().stream()
                 .sorted(Map.Entry.comparingByKey())
                 .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        this.taxTotalsByRate = sorted;
    }

    public BigDecimal getGrandTotal() { return grandTotal; }
    public BigDecimal getSubtotal() { return subtotal; }
    public Map<BigDecimal, BigDecimal> getTaxTotalsByRate() { return taxTotalsByRate; }
}
