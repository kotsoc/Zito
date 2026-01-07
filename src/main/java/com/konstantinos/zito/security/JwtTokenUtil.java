package com.konstantinos.zito.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.konstantinos.zito.model.RestaurantUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

@Component
@Profile("!dev")
public class JwtTokenUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${zito.app.jwtSecret}")
    private String jwtSecret;

    @Value("${zito.app.jwtExpirationMs: 3600000}")
    private int jwtExpirationMs;

    @Value("${zito.app.jwtRefreshExpirationMs: 86400000}")
    private int refreshExpirationMs;

    private SecretKey key;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        // Generate the key once to save CPU cycles on every request
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        // Reusable parser is thread-safe
        this.jwtParser = Jwts.parserBuilder().setSigningKey(key).build();
    }

 private Claims getClaims(String token) {
        return jwtParser.parseClaimsJws(token).getBody();
    }

    public UUID getUserId(String token) {
        return UUID.fromString(getClaims(token).getSubject());
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromJwtToken(String token) {
        return (List<String>) getClaims(token).get("roles");
    }

    public Optional<String> getEmailFromJWTToken(String token) {
        return Optional.ofNullable((String) getClaims(token).get("email"));
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (SecurityException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (UnsupportedJwtException | MalformedJwtException | SignatureException | ExpiredJwtException e) {
        logger.error("Invalid JWT: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public boolean validateJwtRefreshToken(String refreshToken) {
        try {
            Claims claims = getClaims(refreshToken);
            return "refresh".equals(claims.get("type"));
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("Invalid JWT refresh token: {}", e.getMessage());
            return false;
        }
    }

    public ResponseCookie generateRefreshJwtCookie(RestaurantUserDetails userDetails) {
        return buildRefreshCookie(generateRefreshTokenFromUserId(
                userDetails.getId(),
                userDetails.getEmail(),
                userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList()));
    }

    public ResponseCookie generateRefreshJwtCookie(RestaurantUser user) {
        return buildRefreshCookie(generateRefreshTokenFromUserId(
                user.getId(),
                user.getEmail(),
                user.getRoles().stream().toList()));
    }

    private ResponseCookie buildRefreshCookie(String token) {
        return ResponseCookie.from("refreshToken", token)
                .path("/user/refresh")
                .maxAge(refreshExpirationMs / 1000)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie generateClearRefreshCookie() {
        return ResponseCookie.from("refreshToken", "")
                .path("/user/refresh")
                .httpOnly(true)
                .secure(false)
                .maxAge(0)
                .sameSite("Lax").build();
    }

    public String generateAccessToken(RestaurantUser user) {
        return generateAccessTokenFromUserId(
                user.getId(),
                user.getEmail(),
                user.getRoles().stream().toList());
    }

    public String generateAccessToken(RestaurantUserDetails userDetails) {
        return generateAccessTokenFromUserId(
                userDetails.getId(),
                userDetails.getEmail(),
                userDetails.getAuthorities().stream().map(a -> a.getAuthority()).toList());
    }

public String generateAccessTokenFromUserId(UUID userId, String email, List<String> authorities) {
        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .claim("roles", authorities)
            .claim("email", email)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }

    public String generateRefreshTokenFromUserId(UUID userId, String email, List<String> authorities) {
        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(new Date())
            .setExpiration(new Date((new Date()).getTime() + refreshExpirationMs))
            .signWith(key, SignatureAlgorithm.HS256)
            .claim("type", "refresh")
            .compact();
    }
}
