package com.example.zito.security;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.zito.model.RestaurantUser;
import com.example.zito.repositories.UserRepository;

@Service
public class MongoUserDetailsService implements UserDetailsService {
    private UserRepository userRepository;

    public MongoUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        RestaurantUser user = userRepository.findByUsername(username);
        Set<SimpleGrantedAuthority> grantedRoles = new HashSet<>();

        user.getRoles().forEach(role -> {
            grantedRoles.add(new SimpleGrantedAuthority(role));
        });

        return new User(user.getUsername(), user.getPassword(), grantedRoles);
    }

}
