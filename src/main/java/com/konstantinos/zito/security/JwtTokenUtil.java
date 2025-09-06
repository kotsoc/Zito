package com.konstantinos.zito.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

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


    public String getUserNameFromJwtToken(String token) {
        Key privateKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder().setSigningKey(privateKey).build().parseClaimsJws(token).getBody().getSubject();
    }

    public List<String> getRolesFromJwtToken(String token) {
        Key privateKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return (List<String>) Jwts.parserBuilder().setSigningKey(privateKey).build().parseClaimsJws(token).getBody()
                .get("roles");
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

    public ResponseCookie generateRefreshJwtCookie(UserDetails userDetails) {
        String refreshjwt = generateRefreshTokenFromUsername(userDetails.getUsername(),
                userDetails.getAuthorities().stream().map(item -> item.getAuthority()).collect(Collectors.toList()));
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshjwt)
                .path("/")
                .maxAge(refreshExpirationMs / 1000)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax").build();
        return cookie;
    }

    public ResponseCookie generateClearRefreshCookie() {
        return ResponseCookie.from("refreshToken", "")
                .path("/")
                .httpOnly(true)
                .secure(false)
                .maxAge(0)
                .sameSite("Lax").build();
    }

    public String generateTokenFromUsername(String username, List<String> authorities) {
        Key privateKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(privateKey, SignatureAlgorithm.HS256)
                .claim("roles", authorities)
                .compact();
    }

    
    public String generateRefreshTokenFromUsername(String username, List<String> authorities) {
        Key privateKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + refreshExpirationMs))
                .signWith(privateKey, SignatureAlgorithm.HS256)
                .claim("roles", authorities)
                .compact();
    }
}
