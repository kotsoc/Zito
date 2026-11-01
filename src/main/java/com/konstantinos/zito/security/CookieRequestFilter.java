package com.konstantinos.zito.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.konstantinos.zito.model.dto.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class CookieRequestFilter extends OncePerRequestFilter {
    private final List<String> origins;
    private final ObjectMapper mapper;

    public CookieRequestFilter(@Value("${zito.app.cors.allowed-origins:}") List<String> origins,
            ObjectMapper mapper) {
        this.origins = origins;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if ("POST".equals(request.getMethod()) && ("/user/refresh".equals(path) || "/user/signout".equals(path))) {
            String origin = request.getHeader("Origin");
            if (!"1".equals(request.getHeader("X-CSRF-Protection"))
                    || (origin != null && !origins.contains(origin))) {
                response.setStatus(403);
                response.setContentType("application/json");
                String traceId = org.slf4j.MDC.get("traceId");
                mapper.writeValue(response.getOutputStream(), new ApiError("CSRF_REJECTED",
                        traceId == null ? java.util.UUID.randomUUID().toString() : traceId));
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
