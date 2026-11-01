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

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Autowired private com.konstantinos.zito.repositories.OrderRepository orderRepository;
    @Autowired private com.konstantinos.zito.repositories.MenuItemRepository menuItemRepository;

    @Test
    void competingHttpEditsReturnOneSuccessAndOneConflict() throws Exception {
        var admin = new com.konstantinos.zito.model.RestaurantUser("concurrency@example.com");
        admin.addRole("ROLE_ADMIN");
        admin.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(admin);
        var login = new LoginRequest();
        login.setEmail(admin.getEmail());
        login.setPassword("password123");
        var signedIn = restTemplate.postForEntity("/user/signin", login, LoginResponse.class);
        var item = new com.konstantinos.zito.model.MenuItem();
        item.setName("Concurrency fixture");
        item.setGrossPrice(new java.math.BigDecimal("10.00"));
        item.setTaxRate(java.math.BigDecimal.ZERO);
        item = menuItemRepository.save(item);
        var order = orderRepository.save(new com.konstantinos.zito.model.Order());
        var request = new com.konstantinos.zito.model.dto.CreateOrderRequest();
        request.setVersion(order.getVersion());
        request.setCartId(java.util.UUID.randomUUID());
        request.setOrderPointId("table-1");
        request.setCartItems(java.util.List.of(new com.konstantinos.zito.model.dto.CartItemRequest(
                item.getId(), 1, "", java.util.List.of(), 0)));
        var headers = new HttpHeaders();
        headers.setBearerAuth(signedIn.getBody().getAccessToken());
        var entity = new HttpEntity<>(request, headers);
        String path = "/api/v1/orders/waiter/" + order.getId();
        var first = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                restTemplate.exchange(path, HttpMethod.PUT, entity, String.class));
        var second = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                restTemplate.exchange(path, HttpMethod.PUT, entity, String.class));
        var responses = java.util.List.of(first.get(20, java.util.concurrent.TimeUnit.SECONDS),
                second.get(20, java.util.concurrent.TimeUnit.SECONDS));
        assertThat(responses.stream().map(response -> response.getStatusCode().value()).toList())
                .containsExactlyInAnyOrder(201, 409);
        var conflict = responses.stream().filter(response -> response.getStatusCode().value() == 409).findFirst().orElseThrow();
        assertThat(objectMapper.readTree(conflict.getBody()).path("code").asText()).isEqualTo("CONFLICT");
        assertThat(objectMapper.readTree(conflict.getBody()).path("traceId").asText()).isNotBlank();
    }

    @Test
    void exportedContractMatchesCurrentAuthAndCategoryRoutes() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var spec = objectMapper.readTree(response.getBody());
        assertThat(spec.path("openapi").asText()).startsWith("3.");
        assertThat(spec.at("/paths/~1user~1signout/post").isMissingNode()).isFalse();
        assertThat(spec.at("/paths/~1user~1signout/get").isMissingNode()).isTrue();
        assertThat(spec.at("/paths/~1api~1v1~1categories~1by-name~1{name}/get/responses/200").isMissingNode()).isFalse();
        assertThat(spec.at("/paths/~1api~1v1~1categories~1{id}/get/responses/200").isMissingNode()).isFalse();
        assertThat(spec.at("/components/schemas/UserResponse/properties/password").isMissingNode()).isTrue();
        assertThat(spec.at("/paths/~1user~1refresh/post/security/0/csrfHeader").isArray()).isTrue();
        assertThat(spec.at("/components/schemas/OrderStatusRequest/required").toString()).contains("version");
    }

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
    restTemplate.postForEntity("/user/signup", regRequest, String.class);

    // 2. LOGIN
    LoginRequest loginRequest = new LoginRequest();
    loginRequest.setEmail("refresh@test.com");
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
    refreshHeaders.set("X-CSRF-Protection", "1");
    refreshHeaders.add(HttpHeaders.COOKIE, cookieValue); // Send the cookie back
    HttpEntity<Void> refreshEntity = new HttpEntity<>(refreshHeaders);

    ResponseEntity<RefreshResponse> refreshResponse = restTemplate.exchange(
            "/user/refresh", HttpMethod.POST, refreshEntity, RefreshResponse.class);

    assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(refreshResponse.getBody().getAccessToken()).isNotNull();
    String rotatedCookie = refreshResponse.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    assertThat(rotatedCookie).isNotEqualTo(cookieValue);

    ResponseEntity<String> replay = restTemplate.exchange(
            "/user/refresh", HttpMethod.POST, refreshEntity, String.class);
    assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    HttpHeaders rotatedHeaders = new HttpHeaders();
    rotatedHeaders.set("X-CSRF-Protection", "1");
    rotatedHeaders.add(HttpHeaders.COOKIE, rotatedCookie);
    assertThat(restTemplate.exchange("/user/refresh", HttpMethod.POST,
            new HttpEntity<Void>(rotatedHeaders), String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    
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

    // A fresh sign-in creates a separate family; POST logout revokes both credentials.
    ResponseEntity<LoginResponse> secondLogin = restTemplate.postForEntity("/user/signin", loginRequest, LoginResponse.class);
    String logoutCookie = secondLogin.getHeaders().getFirst(HttpHeaders.SET_COOKIE).split(";")[0];
    HttpHeaders logoutHeaders = new HttpHeaders();
    logoutHeaders.setBearerAuth(secondLogin.getBody().getAccessToken());
    logoutHeaders.set("X-CSRF-Protection", "1");
    logoutHeaders.add(HttpHeaders.COOKIE, logoutCookie);
    assertThat(restTemplate.exchange("/user/signout", HttpMethod.POST,
            new HttpEntity<Void>(logoutHeaders), String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    HttpHeaders afterLogout = new HttpHeaders();
    afterLogout.set("X-CSRF-Protection", "1");
    afterLogout.add(HttpHeaders.COOKIE, logoutCookie);
    assertThat(restTemplate.exchange("/user/refresh", HttpMethod.POST,
            new HttpEntity<Void>(afterLogout), String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    assertThat(restTemplate.exchange("/user/me", HttpMethod.GET,
            new HttpEntity<Void>(logoutHeaders), String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
}

    @Test
    @DisplayName("Login fails with wrong password")
    void loginFailsWithWrongPassword() {
        // Create user directly in DB
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("wrongpass@test.com");
        restTemplate.postForEntity("/user/signup", reg, String.class);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("wrongpass@test.com");
        loginRequest.setPassword("incorrect");

        ResponseEntity<String> response = restTemplate.postForEntity("/user/signin", loginRequest, String.class);
        
        // This triggers the real AuthenticationManager/Provider
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
