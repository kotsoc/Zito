package com.konstantinos.zito.integration;

import com.konstantinos.zito.model.dto.LoginRequest;
import com.konstantinos.zito.model.dto.LoginResponse;
import com.konstantinos.zito.model.dto.RefreshResponse;
import com.konstantinos.zito.model.dto.RegisterRequest;
import com.konstantinos.zito.repositories.UserRepository;

// Removed incorrect import for @Container
import org.testcontainers.junit.jupiter.Container;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test") // Ensures @Profile("!dev") is active
@Testcontainers // Automatically starts and stops containers
class AuthenticationIntegrationTest {

    // Define the MongoDB container
    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0")
            .withExposedPorts(27017);

            
    // This dynamically points Spring Data MongoDB to the container's random port
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

@Test
@DisplayName("Full Auth Flow: Register -> Login -> Refresh -> Logout")
void fullAuthenticationLifecycle() {
    // 1. REGISTER
    RegisterRequest regRequest = new RegisterRequest();
    regRequest.setEmail("refresh@test.com");
    regRequest.setPassword("password123");
    regRequest.setPhoneNumber("123456");
    restTemplate.postForEntity("/user/register", regRequest, String.class);

    // 2. LOGIN
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setUsername("refresh@test.com");
    loginRequest.setPassword("password123");

    ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
            "/user/signin", loginRequest, LoginResponse.class);

    // Capture the Refresh Token from the Set-Cookie header
    String setCookieHeader = loginResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    assertThat(setCookieHeader).contains("refreshToken");
    
    // Extract just the name=value part (e.g., "refreshToken=xyz...")
    String cookieValue = setCookieHeader.split(";")[0];

    // 3. REFRESH TOKEN
    HttpHeaders refreshHeaders = new HttpHeaders();
    refreshHeaders.add(HttpHeaders.COOKIE, cookieValue); // Send the cookie back
    HttpEntity<Void> refreshEntity = new HttpEntity<>(refreshHeaders);

    ResponseEntity<RefreshResponse> refreshResponse = restTemplate.exchange(
            "/user/refresh", HttpMethod.GET, refreshEntity, RefreshResponse.class);

    assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(refreshResponse.getBody().getAccessToken()).isNotNull();
    
    // New Access Token for subsequent calls
    String newAccessToken = refreshResponse.getBody().getAccessToken();

    // 4. ACCESS PROTECTED (Using the refreshed token)
    HttpHeaders authHeaders = new HttpHeaders();
    authHeaders.setBearerAuth(newAccessToken);
    authHeaders.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> updateBody = new HashMap<>();
    updateBody.put("email", "refresh@test.com");
    updateBody.put("phoneNumber", "111222");
    updateBody.put("roles", Set.of("ROLE_GUEST"));

    HttpEntity<Map<String, Object>> updateEntity = new HttpEntity<>(updateBody, authHeaders);
    ResponseEntity<String> updateResponse = restTemplate.exchange(
            "/user/update", HttpMethod.PUT, updateEntity, String.class);

    // Still 403 because we are GUEST, but 403 proves the token was accepted!
    assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
}

    @Test
    @DisplayName("Login fails with wrong password")
    void loginFailsWithWrongPassword() {
        // Create user directly in DB
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("wrongpass@test.com");
        restTemplate.postForEntity("/user/register", reg, String.class);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("wrongpass@test.com");
        loginRequest.setPassword("incorrect");

        ResponseEntity<String> response = restTemplate.postForEntity("/user/signin", loginRequest, String.class);
        
        // This triggers the real AuthenticationManager/Provider
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}