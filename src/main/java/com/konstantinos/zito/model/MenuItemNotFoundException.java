package com.konstantinos.zito.model;

public class MenuItemNotFoundException extends RuntimeException {
    public MenuItemNotFoundException() {
        super();
    }

    public MenuItemNotFoundException(String msg) {
        super(msg);
    }

    public MenuItemNotFoundException(String msg, Throwable e) {
        super(msg, e);
    }

}
