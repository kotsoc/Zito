package com.konstantinos.zito.model.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.konstantinos.zito.model.OrderStatus;
import com.konstantinos.zito.model.TaxEntry;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderResponse {
    private UUID id;
    private String orderPointId; // e.g. table number or reservation ID
    private UUID waiterId; //user or waiter who created the order optional
    private Date createdAt;
    private Date latestUpdate;

    // Financials
    private String currency;
    private BigDecimal grossTotal;        // total after discount + tax
    private BigDecimal netTotal;     // discounted net total
    private List<TaxEntry> taxTotals;
    private BigDecimal discount;
    private BigDecimal tip;

    private List<OrderItemResponse> orderItems;
    private boolean paid;
    private OrderStatus status;

    private String guestEmail;
    private String guestPhone;
}