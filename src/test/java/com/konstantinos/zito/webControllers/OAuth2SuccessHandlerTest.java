package com.konstantinos.zito.webControllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.konstantinos.zito.model.AuthProvider;
import com.konstantinos.zito.model.RestaurantUser;
import com.konstantinos.zito.model.dto.LoginResponse;
import com.konstantinos.zito.repositories.UserRepository;
import com.konstantinos.zito.security.JwtTokenUtil;
import com.konstantinos.zito.webControllers.OAuth2SuccessHandler;

@ExtendWith(MockitoExtension.class)
public class OAuth2SuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private OAuth2User oAuth2User;

    @InjectMocks
    private OAuth2SuccessHandler successHandler;

    private StringWriter responseWriter;

    @BeforeEach
    public void setup() throws Exception {
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    public void testOnAuthenticationSuccess_existingUser() throws Exception {
        // Mock OAuth2 user attributes
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("existing@gmail.com");
        when(oAuth2User.getAttribute("sub")).thenReturn("providerId123");

        // Existing user in DB
        RestaurantUser existingUser = new RestaurantUser();
        existingUser.setEmail("existing@gmail.com");
        existingUser.setRoles(Set.of("ROLE_USER"));
        existingUser.setProvider(AuthProvider.LOCAL); // will be updated
        when(userRepository.findByEmail("existing@gmail.com")).thenReturn(Optional.of(existingUser));

        // JWT token and refresh cookie
        when(jwtTokenUtil.generateAccessToken(any(RestaurantUser.class))).thenReturn("jwt-token");
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "refresh-jwt").build();
        when(jwtTokenUtil.generateRefreshJwtCookie(any(RestaurantUser.class))).thenReturn(cookie);

        // Call handler
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Verify provider was updated
        assertEquals(AuthProvider.GOOGLE, existingUser.getProvider());

        // Verify response JSON contains JWT token
        ObjectMapper mapper = new ObjectMapper();
        LoginResponse loginResponse = mapper.readValue(responseWriter.toString(), LoginResponse.class);
        assertEquals("jwt-token", loginResponse.getAccessToken());
        assertEquals("existing@gmail.com", loginResponse.getUsername());
        assertEquals(1, loginResponse.getRoles().size());
        assert(loginResponse.getRoles().contains("ROLE_USER"));

        // Verify refresh cookie is set
        verify(response).addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // Verify userRepository.save() was called for provider update
        verify(userRepository, times(1)).save(existingUser);
    }

    @Test
    public void testOnAuthenticationSuccess_newUser() throws Exception {
        // Mock OAuth2 user attributes
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("newuser@gmail.com");
        when(oAuth2User.getAttribute("sub")).thenReturn("providerId456");

        // No existing user
        when(userRepository.findByEmail("newuser@gmail.com")).thenReturn(Optional.empty());

        // Mock save() to return the user
        when(userRepository.save(any(RestaurantUser.class))).thenAnswer(i -> i.getArguments()[0]);

        // JWT token and refresh cookie
        when(jwtTokenUtil.generateAccessToken(any(RestaurantUser.class))).thenReturn("jwt-token-new");
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "refresh-jwt-new").build();
        when(jwtTokenUtil.generateRefreshJwtCookie(any(RestaurantUser.class))).thenReturn(cookie);

        // Call handler
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // Capture saved user
        verify(userRepository).save(any(RestaurantUser.class));

        // Verify response JSON contains JWT token
        ObjectMapper mapper = new ObjectMapper();
        LoginResponse loginResponse = mapper.readValue(responseWriter.toString(), LoginResponse.class);
        assertEquals("jwt-token-new", loginResponse.getAccessToken());
        assertEquals("newuser@gmail.com", loginResponse.getUsername());
        assertEquals(1, loginResponse.getRoles().size());
        assert(loginResponse.getRoles().contains("ROLE_GUEST"));

        // Verify refresh cookie is set
        verify(response).addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
