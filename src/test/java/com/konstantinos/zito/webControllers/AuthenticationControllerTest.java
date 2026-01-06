package com.konstantinos.zito.webControllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.konstantinos.zito.model.RestaurantUser;
import com.konstantinos.zito.model.dto.LoginRequest;
import com.konstantinos.zito.model.dto.LoginResponse;
import com.konstantinos.zito.model.dto.RegisterRequest;
import com.konstantinos.zito.repositories.UserRepository;
import com.konstantinos.zito.security.JwtTokenUtil;
import com.konstantinos.zito.security.RestaurantUserDetails;
import com.konstantinos.zito.services.TokenInvalidatorService;

@ExtendWith(MockitoExtension.class)
public class AuthenticationControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenInvalidatorService tokenInvalidatorService;

    @InjectMocks
    private AuthenticationController authenticationController;

    @Test
    void testSignIn_success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("test@mail.com");
        request.setPassword("password");

        UUID userId = UUID.randomUUID();

        RestaurantUserDetails userDetails = new RestaurantUserDetails(
                userId,
                "test@mail.com",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        ResponseCookie jwtCookie =
                ResponseCookie.from("refreshToken", "jwt-value").build();

        when(authenticationManager.authenticate(any()))
                .thenReturn(authentication);

        when(jwtTokenUtil.generateAccessToken(userDetails))
                .thenReturn("token-value");

        when(jwtTokenUtil.generateRefreshJwtCookie(userDetails))
                .thenReturn(jwtCookie);

        // Act
        ResponseEntity<LoginResponse> response =
                authenticationController.signIn(request);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(
                jwtCookie.toString(),
                response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)
        );

        LoginResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("token-value", body.getAccessToken());
        assertEquals("test@mail.com", body.getUsername());
        assertEquals(List.of("ROLE_USER"), body.getRoles());
    }


    @Test
    public void testCreateRestaurauntUser() {
        // Create a new RestaurantUser object
        RegisterRequest userRequest = new RegisterRequest();
        userRequest.setEmail("test");
        userRequest.setPassword("password");
        userRequest.setPhoneNumber("12345678");

        RestaurantUser newUser = new RestaurantUser();
        newUser.setEmail(userRequest.getEmail());
        newUser.setPhoneNumber(userRequest.getPhoneNumber());

        // Mock the userRepository.save() method to return the user object
        when(userRepository.save(any(RestaurantUser.class))).thenReturn(newUser);

        // Call the createRestaurauntUser() method
        ResponseEntity<RestaurantUser> responseEntity = authenticationController.createRestaurauntUser(userRequest);

        // Verify that the response entity has a status of CREATED
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        // Verify that the response entity has the correct user object in the body
        RestaurantUser createdUser = responseEntity.getBody();
        assertEquals("test", createdUser.getEmail());
        assertEquals("12345678", createdUser.getPhoneNumber());
        assertEquals(1, createdUser.getRoles().size());
        assertTrue(createdUser.getRoles().contains("ROLE_GUEST"));
    }

    @Test
    public void testUpdateWaiter() {
        // Create a new RestaurantUser object with updated values
        RestaurantUser updatedUser = new RestaurantUser();
        UUID userUuid = UUID.randomUUID();
        updatedUser.setId(userUuid);
        updatedUser.setEmail("test2");
        updatedUser.setPassword("password2");
        updatedUser.setPhoneNumber("98765432");
        updatedUser.addRole("ROLE_WAITER");
        updatedUser.addRole("ROLE_COOK");

        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.of(updatedUser));

        // Call the updateWaiter() method
        ResponseEntity<RestaurantUser> responseEntity = authenticationController.updateWaiter(updatedUser);

        // Verify that the response entity has a status of OK
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // Verify that the response entity has the correct user object in the body
        RestaurantUser updatedResponseUser = responseEntity.getBody();
        assertEquals(userUuid, updatedResponseUser.getId());
        assertEquals("test2", updatedResponseUser.getEmail());
        assertEquals("98765432", updatedResponseUser.getPhoneNumber());
        assertEquals(2, updatedResponseUser.getRoles().size());
        assertTrue(updatedResponseUser.getRoles().contains("ROLE_WAITER"));
        assertTrue(updatedResponseUser.getRoles().contains("ROLE_COOK"));
    }

     @Test
    public void testSignOut() {
        // Mock an authentication object with a token
        Authentication authentication = mock(Authentication.class);

        when(authentication.getCredentials()).thenReturn("token-value");

        ResponseCookie refreshCookie = ResponseCookie.from("refreshCookie", "")
        .build();
        when(jwtTokenUtil.generateClearRefreshCookie()).thenReturn(refreshCookie);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Call the signOut() method
        ResponseEntity<Void> responseEntity = authenticationController.signOut();

        // Verify that the response entity has a status of OK
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        // Verify that the response cookie
        assertEquals("refreshCookie=", responseEntity.getHeaders().getFirst(HttpHeaders.SET_COOKIE));

        // Verify that the tokenInvalidatorService.invalidateToken() method was called with the token
        verify(tokenInvalidatorService, times(1)).invalidateToken("token-value");
    }
}
