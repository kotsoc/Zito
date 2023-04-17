package com.example.zito.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Waiter")
public class Waiter {

    @Id
    String id;

    Integer phoneNumber;

    String name;

    public Waiter() {
    }

    public Waiter(Integer phoneNumber, String name) {
        this.phoneNumber = phoneNumber;
        this.name = name;
    }

    public Waiter(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(Integer phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
