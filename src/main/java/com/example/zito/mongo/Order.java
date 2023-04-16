package com.example.zito.mongo;

import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "orders")
public class Order {
    @Id
    private String id;

    private int orderid;
    private int table;
    private Date order_time;
    private double total;
    private boolean paid;

    @DBRef
    private Waiter waiter;

    @DBRef
    private List<MenuItem> items;

    public Order() {
    }

    public Order(int orderid, int table, Date order_time, double total, Waiter server, List<MenuItem> items,
            boolean paid) {
        this.orderid = orderid;
        this.table = table;
        this.order_time = order_time;
        this.total = total;
        this.waiter = server;
        this.items = items;
        this.paid = paid;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getOrderid() {
        return orderid;
    }

    public void setOrderid(int orderid) {
        this.orderid = orderid;
    }

    public int getTable() {
        return table;
    }

    public void setTable(int table) {
        this.table = table;
    }

    public Date getOrder_time() {
        return order_time;
    }

    public void setOrder_time(Date order_time) {
        this.order_time = order_time;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public Waiter getWaiter() {
        return waiter;
    }

    public void setWaiter(Waiter server) {
        this.waiter = server;
    }

    public List<MenuItem> getItems() {
        return items;
    }

    public void setItems(List<MenuItem> items) {
        this.items = items;
    }
}