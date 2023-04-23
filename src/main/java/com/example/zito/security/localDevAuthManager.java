package com.example.zito.security;

import java.util.ArrayList;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Configuration
@Profile("dev")
public class localDevAuthManager implements AuthenticationManager {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        System.out.println("edwwww " + authentication.getCredentials());
        if (authentication.getPrincipal().toString().equals("admin")
                && authentication.getCredentials().toString().equals("admin")) {
            var authoritiesList = new ArrayList<SimpleGrantedAuthority>();
            authoritiesList.add(new SimpleGrantedAuthority("admin"));
            return new UsernamePasswordAuthenticationToken(authentication.getPrincipal(),
                    authentication.getCredentials(), authoritiesList);
        }
        return authentication;
    }

}
