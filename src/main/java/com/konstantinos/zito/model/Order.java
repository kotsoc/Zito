package com.konstantinos.zito.model;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.Setter;
import lombok.AccessLevel;

@Document(collection = "Order")
@Data
public class Order {

    @Id
    @Setter(AccessLevel.NONE)
    private UUID id;

    private int tableNumber;
    private Date order_time;
    private Date latest_update;
    private double total;
    private boolean paid;
    private OrderStatus status;

    @Indexed
    private String waiterName;

    // @DBRef
    private Map<String, Integer> items;

    public Order() {
    }

    public Order(int table, Date order_time, double total, String waiterName, Map<String, Integer> items,
            boolean paid) {
        this.id = UUID.randomUUID();
        this.tableNumber = table;
        this.order_time = order_time;
        this.total = total;
        this.waiterName = waiterName;
        this.items = items;
        this.paid = paid;
        this.status = OrderStatus.OPEN;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }
}