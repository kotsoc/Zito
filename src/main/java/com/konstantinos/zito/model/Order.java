package com.konstantinos.zito.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@Document(collection = "orders")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {

    @Id
    @Setter(AccessLevel.NONE)
    private UUID id;

    private int tableNumber;
    private Date created_At;
    private Date latest_update;

    private BigDecimal total;
    private BigDecimal tax;
    private BigDecimal discount;
    private BigDecimal tip;

    private List<CartItem> cartItems;
    private boolean paid;
    private OrderStatus status;

    @Indexed
    private String waiterName;

    private String guestEmail; //optional
    private String guestPhone; //optional

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }
}