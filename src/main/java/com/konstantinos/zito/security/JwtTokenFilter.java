package com.konstantinos.zito.security;

import java.io.IOException;
import java.util.List;
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
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Skip JWT validation for these endpoints
        return path.equals("/user/signin")
            || path.equals("/user/register")
            || path.equals("/user/refresh")
            || path.startsWith("/v3/")
            || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            final String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header == null || (header.isEmpty() || !header.startsWith("Bearer "))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or Lacking Authorization");
                return;
            }

            final String jwtToken = header.split(" ")[1].trim();
            if (!jwtTokenUtil.validateJwtToken(jwtToken) || tokenInvalidatorService.isInvalid(jwtToken)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
                return;
            }

            String username = jwtTokenUtil.getUserNameFromJwtToken(jwtToken);
            List<GrantedAuthority> authorities = jwtTokenUtil.getRolesFromJwtToken(jwtToken).stream()
                                                .map(role -> new SimpleGrantedAuthority(role))
                                                .collect(Collectors.toList());
            var authentication = new UsernamePasswordAuthenticationToken(username, jwtToken, authorities);
            
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authnentication Failed");
            return;
        }
        filterChain.doFilter(request, response);

    }
}
