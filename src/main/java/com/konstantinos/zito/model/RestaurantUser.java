package com.konstantinos.zito.model;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Document(collection = "User")
@Data
public class RestaurantUser {

    @Id
    String id;

    String phoneNumber;

    @Indexed(unique = true)
    @NotBlank
    @Size(min = 6, max = 20, message = "Username must be between 3 and 20 characters")
    String username;

    @Size(min = 5, max = 25, message = "Password must be between 5 and 25 characters")
    @NotBlank
    String password;

    Set<String> roles;

    public RestaurantUser() {
        this.roles = new HashSet<String>();
    }

    public RestaurantUser(String phoneNumber, String name) {
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
