package com.konstantinos.zito.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.konstantinos.zito.model.RestaurantUser;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;

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


    public UUID getUserId(String token) {
        Key privateKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return UUID.fromString(Jwts.parserBuilder().setSigningKey(privateKey).build().parseClaimsJws(token).getBody().getSubject());
    }

    public List<String> getRolesFromJwtToken(String token) {
        Key privateKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return (List<String>) Jwts.parserBuilder().setSigningKey(privateKey).build().parseClaimsJws(token).getBody()
                .get("roles");
    }

    public Optional<String> getEmailFromJWTToken(String token) {
        Key privateKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Optional.ofNullable(String.valueOf(Jwts.parserBuilder().setSigningKey(privateKey).build().parseClaimsJws(token).getBody().get("email")));
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Key privateKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Jwts.parserBuilder().setSigningKey(privateKey).build().parseClaimsJws(authToken);
            return true;
        } catch (SecurityException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public boolean validateJwtRefreshToken(String refreshToken) {
        try {
            Key privateKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            var claims = Jwts.parserBuilder().setSigningKey(privateKey).build().parseClaimsJws(refreshToken).getBody();
            return validateJwtToken(refreshToken) && "refresh".equals(claims.get("type"));
        } catch (Exception e) {
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
        Key privateKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
            .signWith(privateKey, SignatureAlgorithm.HS256)
            .claim("roles", authorities)
            .claim("email", email)
            .compact();
    }

    public String generateRefreshTokenFromUserId(UUID userId, String email, List<String> authorities) {
        Key privateKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
            .setSubject(userId.toString())
            .setIssuedAt(new Date())
            .setExpiration(new Date((new Date()).getTime() + refreshExpirationMs))
            .signWith(privateKey, SignatureAlgorithm.HS256)
            .claim("type", "refresh")
            .compact();
    }
}
