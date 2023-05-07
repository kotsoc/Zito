package com.konstantinos.zito.security;

import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.konstantinos.zito.model.RestaurantUser;
import com.konstantinos.zito.repositories.UserRepository;

@Service
@Profile("!dev")
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
