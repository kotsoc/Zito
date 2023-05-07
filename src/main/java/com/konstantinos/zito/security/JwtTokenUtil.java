package com.konstantinos.zito.security;

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

@Component
@Profile("!dev")
public class JwtTokenUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${zito.app.jwtSecret}")
    private String jwtSecret;

    @Value("${zito.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(token).getBody().getSubject();
    }

    public List<String> getRolesFromJwtToken(String token) {
        return (List<String>) Jwts.parserBuilder().setSigningKey(jwtSecret).build().parseClaimsJws(token).getBody()
                .get("roles");
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
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

    public ResponseCookie generateJwtCookie(UserDetails userDetails) {
        String jwt = generateTokenFromUsername(userDetails.getUsername(),
                userDetails.getAuthorities().stream().map(item -> item.getAuthority()).collect(Collectors.toList()));
        ResponseCookie cookie = ResponseCookie.from("jwtCookie", jwt).path("/api").maxAge(24 * 60 * 60).httpOnly(true)
                .build();
        return cookie;
    }

    public String generateTokenFromUsername(String username, List<String> authorities) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS512, jwtSecret)
                .claim("roles", authorities)
                .compact();
    }
}
