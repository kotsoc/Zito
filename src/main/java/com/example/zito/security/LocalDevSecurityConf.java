package com.example.zito.security;

import java.util.ArrayList;

import org.springframework.context.annotation.Bean;

import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("dev")
public class LocalDevSecurityConf implements AuthenticationManager {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests()
                .anyRequest().authenticated().and().httpBasic(withDefaults());

        return http.build();
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        System.out.println("edwwww " + authentication.getCredentials());
        if (authentication.getPrincipal().toString().equals("babas")
                && authentication.getCredentials().toString().equals("babas")) {
            var authoritiesList = new ArrayList<SimpleGrantedAuthority>();
            authoritiesList.add(new SimpleGrantedAuthority("admin"));
            return new UsernamePasswordAuthenticationToken(authentication.getPrincipal(),
                    authentication.getCredentials(), authoritiesList);
        }
        return authentication;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(8);
    }

}
