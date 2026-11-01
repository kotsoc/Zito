package com.konstantinos.zito.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {

        writeError(response, 401, "UNAUTHORIZED");
    }

    public static void writeError(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        String traceId = org.slf4j.MDC.get("traceId");
        new com.fasterxml.jackson.databind.ObjectMapper().writeValue(response.getOutputStream(),
                new com.konstantinos.zito.model.dto.ApiError(code,
                        traceId == null ? java.util.UUID.randomUUID().toString() : traceId));
    }
}
