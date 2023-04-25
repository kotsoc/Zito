package com.example.zito.model;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Document(collection = "user")
@Data
public class RestaurantUser {

    @Id
    String id;

    Integer phoneNumber;

    @Indexed(unique = true)
    @NotBlank
    String username;

    String password;

    Set<String> roles;

    public RestaurantUser() {
    }

    public RestaurantUser(Integer phoneNumber, String name) {
        this.phoneNumber = phoneNumber;
        this.username = name;
    }

    public RestaurantUser(String name) {
        this.username = name;
    }

    public void addRole(String role) {
        this.roles.add(role);
    }
}
