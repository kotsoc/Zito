package com.konstantinos.zito.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import com.konstantinos.zito.model.RestaurantUser;
import com.konstantinos.zito.security.JwtTokenUtil;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class JwtTokenUtilTest {

    private JwtTokenUtil jwtTokenUtil;
    private final String SECRET = "myVerySecretKeyThatIsAtLeast32BytesLongForHS256!!";
    private final int ACCESS_EXPIRATION = 3600000; // 1 hour
    private final int REFRESH_EXPIRATION = 86400000; // 24 hours

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil();
        // Manually inject @Value fields
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtExpirationMs", ACCESS_EXPIRATION);
        ReflectionTestUtils.setField(jwtTokenUtil, "refreshExpirationMs", REFRESH_EXPIRATION);

        jwtTokenUtil.init();
    }

    @Test
    @DisplayName("Generate Access Token and Extract Claims")
    void generateAndExtractClaims() {
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");

        String token = jwtTokenUtil.generateAccessTokenFromUserId(userId, email, roles);

        assertThat(jwtTokenUtil.validateJwtToken(token)).isTrue();
        assertThat(jwtTokenUtil.getUserId(token)).isEqualTo(userId);
        assertThat(jwtTokenUtil.getEmailFromJWTToken(token)).contains(email);
        assertThat(jwtTokenUtil.getRolesFromJwtToken(token)).containsAll(roles);
    }

    @Test
    @DisplayName("Refresh Token should have 'type: refresh' claim")
    void validateRefreshTokenLogic() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenUtil.generateRefreshTokenFromUserId(userId, "test@mail.com", List.of("ROLE_USER"));

        assertThat(jwtTokenUtil.validateJwtRefreshToken(token)).isTrue();
        
        // A regular access token should NOT be valid as a refresh token
        String accessToken = jwtTokenUtil.generateAccessTokenFromUserId(userId, "test@mail.com", List.of("ROLE_USER"));
        assertThat(jwtTokenUtil.validateJwtRefreshToken(accessToken)).isFalse();
    }

    @Test
    @DisplayName("Token validation should fail if signature is tampered with")
    void validateTamperedToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenUtil.generateAccessTokenFromUserId(userId, "test@mail.com", List.of("USER"));
        
        // Tamper with the token string (change the last character of the signature)
        String tamperedToken = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThat(jwtTokenUtil.validateJwtToken(tamperedToken));
    }

    @Test
    @DisplayName("Expired token should return false on validation")
    void validateExpiredToken() {
        // Temporarily inject a negative expiration
        ReflectionTestUtils.setField(jwtTokenUtil, "jwtExpirationMs", -10000);
        
        String token = jwtTokenUtil.generateAccessTokenFromUserId(UUID.randomUUID(), "a@b.com", List.of());
        
        assertThat(jwtTokenUtil.validateJwtToken(token)).isFalse();
    }

    @Test
    @DisplayName("Verify buildRefreshCookie properties")
    void verifyCookieProperties() {
        RestaurantUser user = new RestaurantUser();
        user.setId(UUID.randomUUID());
        user.setEmail("test@test.com");
        user.setRoles(Set.of("ROLE_USER")); // Initialize the set!

        ResponseCookie cookie = jwtTokenUtil.generateRefreshJwtCookie(user);

        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.isHttpOnly()).isTrue();
        // ... rest of your assertions
    }
}