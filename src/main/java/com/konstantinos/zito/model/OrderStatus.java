package com.konstantinos.zito.model;

public enum OrderStatus {
    NEW (0),
    CONFIRMED (1),
    PREPARING (2),
    READY (3),
    COMPLETED (4);

    private final int orderStatusPrecedence;

    private  OrderStatus(int precedence){
        this.orderStatusPrecedence = precedence;
    }

    public int getPrecedence(){
        return this.orderStatusPrecedence;
    }
}