package com.konstantinos.zito.model.dto;

import java.util.Set;
import java.util.UUID;

import com.konstantinos.zito.model.RestaurantUser;

import lombok.Value;

@Value
public class UserResponse {
    UUID id;
    String email;
    String name;
    String phoneNumber;
    Set<String> roles;

    public static UserResponse from(RestaurantUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getPhoneNumber(),
                user.getRoles() == null ? Set.of() : Set.copyOf(user.getRoles()));
    }
}
