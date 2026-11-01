package com.konstantinos.zito.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.konstantinos.zito.model.dto.ApiError;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class PublicRequestLimitFilter extends OncePerRequestFilter {
    private final int requestsPerMinute;
    private final int maxBodyBytes;
    private final ObjectMapper mapper;
    private final Map<String, Bucket> buckets = new HashMap<>();
    private final long startedAt = System.nanoTime();
    private record Bucket(long minute, int count) {}

    public PublicRequestLimitFilter(@Value("${zito.app.limits.requests-per-minute:60}") int requestsPerMinute,
            @Value("${zito.app.limits.max-body-bytes:65536}") int maxBodyBytes, ObjectMapper mapper) {
        if (requestsPerMinute < 1 || maxBodyBytes < 1) throw new IllegalArgumentException("Limits must be positive");
        this.requestsPerMinute = requestsPerMinute;
        this.maxBodyBytes = maxBodyBytes;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String path = request.getServletPath();
        boolean auth = path.startsWith("/user/") && "POST".equals(request.getMethod())
                && java.util.Set.of("/user/signin", "/user/signup", "/user/refresh", "/user/signout").contains(path);
        boolean ordering = "POST".equals(request.getMethod()) && "/api/v1/orders".equals(path);
        boolean lookup = "GET".equals(request.getMethod()) && path.matches("/api/v1/orders/[0-9a-fA-F-]{36}");
        if (!auth && !ordering && !lookup) {
            chain.doFilter(request, response);
            return;
        }
        String group = auth ? "auth" : ordering ? "ordering" : "lookup";
        if (!allow(group + ":" + request.getRemoteAddr())) {
            response.setHeader("Retry-After", "60");
            reject(response, 429, "RATE_LIMITED");
            return;
        }
        if (request.getContentLengthLong() > maxBodyBytes) {
            reject(response, 413, "PAYLOAD_TOO_LARGE");
            return;
        }
        byte[] body = request.getInputStream().readNBytes(maxBodyBytes + 1);
        if (body.length > maxBodyBytes) {
            reject(response, 413, "PAYLOAD_TOO_LARGE");
            return;
        }
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override public ServletInputStream getInputStream() {
                var input = new ByteArrayInputStream(body);
                return new ServletInputStream() {
                    @Override public int read() { return input.read(); }
                    @Override public boolean isFinished() { return input.available() == 0; }
                    @Override public boolean isReady() { return true; }
                    @Override public void setReadListener(ReadListener listener) {
                        throw new UnsupportedOperationException("Async request reads are unsupported");
                    }
                };
            }
            @Override public BufferedReader getReader() {
                return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
            }
        }, response);
    }

    private synchronized boolean allow(String key) {
        long minute = (System.nanoTime() - startedAt) / java.util.concurrent.TimeUnit.MINUTES.toNanos(1);
        buckets.entrySet().removeIf(entry -> entry.getValue().minute() != minute);
        Bucket bucket = buckets.get(key);
        if (bucket == null) {
            if (buckets.size() >= 10000) return false;
            buckets.put(key, new Bucket(minute, 1));
            return true;
        }
        if (bucket.count() >= requestsPerMinute) return false;
        buckets.put(key, new Bucket(minute, bucket.count() + 1));
        return true;
    }

    private void reject(HttpServletResponse response, int status, String code) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        String traceId = org.slf4j.MDC.get("traceId");
        mapper.writeValue(response.getOutputStream(), new ApiError(code,
                traceId == null ? java.util.UUID.randomUUID().toString() : traceId));
    }
}
