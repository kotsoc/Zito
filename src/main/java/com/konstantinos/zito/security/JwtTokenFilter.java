package com.konstantinos.zito.security;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import com.konstantinos.zito.services.TokenInvalidatorService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Profile("!dev")
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final TokenInvalidatorService tokenInvalidatorService;

    public JwtTokenFilter(JwtTokenUtil jwtTokenUtil, TokenInvalidatorService tokenInvalidatorService) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.tokenInvalidatorService = tokenInvalidatorService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            
            // 1. Only attempt authentication if the header exists and is a Bearer token
            if (header != null && header.toLowerCase().startsWith("bearer ")) {
                final String jwtToken = header.substring(7).trim();
                System.out.println("Extracted JWT Token: " + jwtToken); // Debug log to verify token extraction
                // 2. Only authenticate if the token is valid and not blacklisted
                if (jwtTokenUtil.validateJwtToken(jwtToken) && !tokenInvalidatorService.isInvalid(jwtToken)) {
                    
                    UUID userId = jwtTokenUtil.getUserId(jwtToken);
                    String email = jwtTokenUtil.getEmailFromJWTToken(jwtToken).orElse("");
                    List<GrantedAuthority> authorities = jwtTokenUtil.getRolesFromJwtToken(jwtToken).stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                                            
                    RestaurantUserDetails principal = new RestaurantUserDetails(userId, email, authorities);
                    var authentication = new UsernamePasswordAuthenticationToken(principal, jwtToken, authorities);
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 3. Put the user's "Nametag" on the request
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            // Log the error, but DO NOT block the request. 
            // Let Spring Security handle the rejection later if this was a protected route.
            logger.error("Cannot set user authentication: {}", e);
        }

        // If it was public, it goes through. If it was private, Spring blocks it.
        filterChain.doFilter(request, response);
    }
}