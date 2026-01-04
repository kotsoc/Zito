package com.konstantinos.zito.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.konstantinos.zito.model.RestaurantUser;

public class RestaurantUserDetails implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final Set<GrantedAuthority> authorities;

    public RestaurantUserDetails(RestaurantUser user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.authorities = user.getRoles().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    public RestaurantUserDetails(UUID userId, String email, List<GrantedAuthority> authorities) {
        this.id = userId;
        this.email = email;
        this.password = null; // Not used in authentication
        this.authorities = authorities.stream().collect(Collectors.toSet());
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email; // Spring uses this for login
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
