package com.konstantinos.zito.model.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.konstantinos.zito.model.OrderStatus;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OrderResponse {
    private UUID id;
    private String orderPointId; // e.g. table number or reservation ID
    private String waiterId; //user or waiter who created the order optional
    private Date createdAt;
    private Date latestUpdate;

    // Financials
    private String currency;
    private BigDecimal total;        // total after discounts
    private BigDecimal subtotal;     // sum of item subtotals
    private BigDecimal tax;
    private BigDecimal discount;
    private BigDecimal tip;

    private List<CartItemResponse> cartItems;
    private boolean paid;
    private OrderStatus status;

    private String guestEmail;
    private String guestPhone;
}