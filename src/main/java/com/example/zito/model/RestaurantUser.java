package com.example.zito.model;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user")
public class RestaurantUser {

    @Id
    String id;

    Integer phoneNumber;

    @Indexed(unique = true)
    String name;

    String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    Set<String> userRoles;

    public RestaurantUser() {
    }

    public RestaurantUser(Integer phoneNumber, String name) {
        this.phoneNumber = phoneNumber;
        this.name = name;
    }

    public RestaurantUser(String name) {
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

    public Set<String> getRoles() {
        return userRoles;
    }

    public void setRoles(Set<String> userRoles) {
        this.userRoles = userRoles;
    }

    public void addRole(String role) {
        this.userRoles.add(role);
    }
}
