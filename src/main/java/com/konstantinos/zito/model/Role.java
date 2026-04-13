package com.konstantinos.zito.model;

import lombok.Getter;

@Getter
public enum Role {
    ADMIN("ROLE_ADMIN"),
    USER("ROLE_USER"),
    WAITER("ROLE_WAITER"),
    MANAGER("ROLE_MANAGER"),
    KITCHEN("ROLE_KITCHEN"),
    GUEST("ROLE_GUEST");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    public static Role fromString(String roleString) {
        for (Role role : Role.values()) {
            if (role.authority.equalsIgnoreCase(roleString)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + roleString);
    }

    public static Role fromName(String name) {
        for (Role role : Role.values()) {
            if (role.name().equalsIgnoreCase(name)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role name: " + name);
    }
}
