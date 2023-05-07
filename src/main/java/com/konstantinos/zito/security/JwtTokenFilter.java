package com.example.zito.security;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Profile("!dev")
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final MongoUserDetailsService mongoUserDetails;

    public JwtTokenFilter(JwtTokenUtil jwtTokenUtil, MongoUserDetailsService mongoUserDetails) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.mongoUserDetails = mongoUserDetails;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header == null || (header.isEmpty() || !header.startsWith("Bearer "))) {
                filterChain.doFilter(request, response);
                return;
            }

            final String jwtToken = header.split(" ")[1].trim();
            if (!jwtTokenUtil.validateJwtToken(jwtToken)) {
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtTokenUtil.getUserNameFromJwtToken(jwtToken);

            UserDetails userDetails = mongoUserDetails.loadUserByUsername(username);
            var authentication = new UsernamePasswordAuthenticationToken(userDetails.getUsername(), null,
                    userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);

    }
}
