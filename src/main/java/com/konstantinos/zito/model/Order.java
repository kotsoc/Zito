package com.konstantinos.zito.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.Setter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Document(collection = "orders")
@Data
@AllArgsConstructor
public class Order {

    public Order() {
        this.id = UUID.randomUUID();
        this.created_At = new Date();
        this.latest_update = new Date();
        this.paid = false;
        this.status = OrderStatus.NEW;
    }

    @Id
    @Setter(AccessLevel.NONE)
    private UUID id;
    private String orderPointId; // table, bar, takeaway, etc.
    private UUID waiterId; // Only for orders created by waiters/users
    private UUID cartId; // Only for guest orders

    private String guestEmail; //optional
    private String guestPhone; //optional
    
    private List<OrderItem> orderItems;
    private boolean paid;
    private OrderStatus status;
    private String notes;
    
    @CreatedDate
    private Date created_At;
    @LastModifiedDate
    private Date latest_update;

    // Financial details
    private String currency;
    private BigDecimal orderDiscount; //
    private BigDecimal tip;

    // Calculated Totals
    private BigDecimal discountTotal;      //Calculated at checkout
    private BigDecimal modifierTotal;       //calculated at checkout
    private BigDecimal netTotal;
    private BigDecimal grossTotal;
    private Map<BigDecimal, BigDecimal> taxTotals;
}