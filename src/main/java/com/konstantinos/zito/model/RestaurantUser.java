package com.konstantinos.zito.model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Document(collection = "users")
@Data
public class RestaurantUser {

    @Id
    UUID id = UUID.randomUUID();

    String phoneNumber;

    @Indexed(unique = true)
    @NotBlank
    @Size(min = 6, max = 45, message = "Email must be between 6 and 45 characters")
    @Email
    String email;

    String password;

    private AuthProvider provider;

    private String providerId;

    Set<String> roles;

    public RestaurantUser() {
        this.roles = new HashSet<String>();
    }

    public RestaurantUser(String phoneNumber, String name) {
        this.phoneNumber = phoneNumber;
        this.email = name;
    }

    public RestaurantUser(String name) {
        this.email = name;
    }

    public void addRole(String role) {
        this.roles.add(role);
    }
}
