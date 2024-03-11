package com.konstantinos.zito.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.konstantinos.zito.model.LoginRequest;
import com.konstantinos.zito.model.LoginResponse;
import com.konstantinos.zito.model.RestaurantUser;
import com.konstantinos.zito.repositories.UserRepository;
import com.konstantinos.zito.security.JwtTokenUtil;
import com.konstantinos.zito.services.TokenInvalidatorService;
import com.konstantinos.zito.webControllers.AuthenticationController;

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
    public void testSignIn() {
        // Create a new LoginRequest object
        LoginRequest request = new LoginRequest();
        request.setUsername("test");
        request.setPassword("password");

        // Create a new UserDetails object
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        UserDetails userDetails = new User("test", "password", authorities);

        // Create a new Authentication object
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities());

        // Create a new ResponseCookie object
        ResponseCookie jwtCookie = ResponseCookie.from("jwt", "jwt-value").build();

        // Mock the authenticationManager.authenticate() method to return the
        // authentication object
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        // Mock the jwtTokenUtil.generateJwtCookie() method to return the jwtCookie
        // object
        when(jwtTokenUtil.generateJwtCookie(any(UserDetails.class))).thenReturn(jwtCookie);

        // Call the signIn() method
        ResponseEntity<LoginResponse> responseEntity = authenticationController.signIn(request);

        // Verify that the response entity has a status of OK
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // Verify that the response entity has the correct jwt cookie in the header
        assertEquals(jwtCookie.toString(), responseEntity.getHeaders().getFirst(HttpHeaders.SET_COOKIE));

        // Verify that the response entity has a LoginResponse object in the body with
        // the correct values
        LoginResponse loginResponse = responseEntity.getBody();
        assertEquals("jwt-value", loginResponse.getJwtToken());
        assertEquals("test", loginResponse.getUsername());
        assertEquals(1, loginResponse.getRoles().size());
        assert (loginResponse.getRoles().contains("ROLE_USER"));
    }

    @Test
    public void testCreateRestaurauntUser() {
        // Create a new RestaurantUser object
        RestaurantUser user = new RestaurantUser();
        user.setUsername("test");
        user.setPassword("password");
        user.setPhoneNumber("12345678");
        user.addRole("ROLE_WAITER");

        // Mock the userRepository.save() method to return the user object
        when(userRepository.save(any(RestaurantUser.class))).thenReturn(user);

        // Call the createRestaurauntUser() method
        ResponseEntity<RestaurantUser> responseEntity = authenticationController.createRestaurauntUser(user);

        // Verify that the response entity has a status of CREATED
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        // Verify that the response entity has the correct user object in the body
        RestaurantUser createdUser = responseEntity.getBody();
        assertEquals("test", createdUser.getUsername());
        assertEquals("12345678", createdUser.getPhoneNumber());
        assertEquals(1, createdUser.getRoles().size());
        assertTrue(createdUser.getRoles().contains("ROLE_GUEST"));
    }

    @Test
    public void testUpdateWaiter() {
        // Create a new RestaurantUser object with updated values
        RestaurantUser updatedUser = new RestaurantUser();
        updatedUser.setId("123");
        updatedUser.setUsername("test2");
        updatedUser.setPassword("password2");
        updatedUser.setPhoneNumber("98765432");
        updatedUser.addRole("ROLE_WAITER");
        updatedUser.addRole("ROLE_COOK");

        when(userRepository.findByUsername(anyString()))
                .thenReturn(Optional.of(updatedUser));

        // Call the updateWaiter() method
        ResponseEntity<RestaurantUser> responseEntity = authenticationController.updateWaiter(updatedUser);

        // Verify that the response entity has a status of OK
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // Verify that the response entity has the correct user object in the body
        RestaurantUser updatedResponseUser = responseEntity.getBody();
        assertEquals("123", updatedResponseUser.getId());
        assertEquals("test2", updatedResponseUser.getUsername());
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
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Call the signOut() method
        ResponseEntity<Void> responseEntity = authenticationController.signOut();

        // Verify that the response entity has a status of OK
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        // Verify that the tokenInvalidatorService.invalidateToken() method was called with the token
        verify(tokenInvalidatorService, times(1)).invalidateToken("token-value");
    }
}
