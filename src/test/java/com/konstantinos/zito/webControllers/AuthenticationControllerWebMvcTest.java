package com.konstantinos.zito.webControllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.konstantinos.zito.model.RestaurantUser;
import com.konstantinos.zito.model.dto.LoginRequest;
import com.konstantinos.zito.model.dto.RegisterRequest;
import com.konstantinos.zito.security.JwtTokenUtil;
import com.konstantinos.zito.security.RestaurantUserDetails;
import com.konstantinos.zito.services.TokenInvalidatorService;

import jakarta.servlet.http.Cookie;

import com.konstantinos.zito.repositories.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false) // <--- CRITICAL: Bypasses all security filterrs
@EnableMethodSecurity
class AuthenticationControllerWebMvcTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private UserRepository userRepository;

        @MockitoBean
        private AuthenticationManager authenticationManager;

        @MockitoBean
        private JwtTokenUtil jwtTokenUtil;

        @MockitoBean
        private PasswordEncoder passwordEncoder;

        @MockitoBean
        private TokenInvalidatorService tokenInvalidatorService;

        @AfterEach
        void cleanup() {
                SecurityContextHolder.clearContext();
        }

        /*
         * =========================
         * SIGN IN
         * =========================
         */

        @Test
        void signIn_success() throws Exception {
                UUID userId = UUID.randomUUID();

                RestaurantUserDetails userDetails = new RestaurantUserDetails(
                                userId,
                                "test@mail.com",
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));

                var authentication = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());

                ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", "jwt-value").build();

                when(authenticationManager.authenticate(any()))
                                .thenReturn(authentication);

                when(jwtTokenUtil.generateAccessToken(userDetails))
                                .thenReturn("access-token");

                when(jwtTokenUtil.generateRefreshJwtCookie(userDetails))
                                .thenReturn(refreshCookie);

                LoginRequest request = new LoginRequest();
                request.setUsername("test@mail.com");
                request.setPassword("password");

                mockMvc.perform(post("/user/signin")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(header().string(
                                                HttpHeaders.SET_COOKIE,
                                                refreshCookie.toString()))
                                .andExpect(jsonPath("$.accessToken").value("access-token"))
                                .andExpect(jsonPath("$.username").value("test@mail.com"))
                                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));
        }

        @Test
        void signIn_invalidPrincipal_returns401() throws Exception {
                Authentication authentication = new UsernamePasswordAuthenticationToken("string", null);

                when(authenticationManager.authenticate(any())).thenReturn(authentication);

                mockMvc.perform(post("/user/signin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                { "username":"a","password":"b" }
                                                """))
                                .andExpect(status().isUnauthorized());
        }

        /*
         * =========================
         * REGISTER
         * =========================
         */

        @Test
        void register_createsGuestUser() throws Exception {
                RegisterRequest request = new RegisterRequest();
                request.setEmail("test@mail.com");
                request.setPassword("password");
                request.setPhoneNumber("12345678");

                when(passwordEncoder.encode(any()))
                                .thenReturn("encoded");

                RestaurantUser savedUser = new RestaurantUser();
                savedUser.setEmail("test@mail.com");
                savedUser.setPhoneNumber("12345678");
                savedUser.addRole("ROLE_GUEST");

                when(userRepository.save(any(RestaurantUser.class)))
                                .thenReturn(savedUser);

                mockMvc.perform(post("/user/register")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email").value("test@mail.com"))
                                .andExpect(jsonPath("$.roles[0]").value("ROLE_GUEST"));
        }

        @Test
        void register_duplicateUser_returns409() throws Exception {
                when(userRepository.save(any())).thenThrow(new RuntimeException());

                mockMvc.perform(post("/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "email":"test@mail.com",
                                                  "password":"12345678",
                                                  "phoneNumber":"112345671"
                                                }
                                                """))
                                .andExpect(status().isConflict());
        }

        /*
         * =========================
         * SIGN OUT
         * =========================
         */

        @Test
        void signOut_success() throws Exception {
                // Arrange authentication manually
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                "user",
                                "access-token");

                SecurityContextHolder.getContext().setAuthentication(authentication);

                ResponseCookie clearCookie = ResponseCookie.from("refreshToken", "").build();

                when(jwtTokenUtil.generateClearRefreshCookie())
                                .thenReturn(clearCookie);

                // Act + Assert
                mockMvc.perform(get("/user/signout"))
                                .andExpect(status().isOk())
                                .andExpect(header().string(
                                                HttpHeaders.SET_COOKIE,
                                                clearCookie.toString()));

                verify(tokenInvalidatorService).invalidateToken("access-token");
        }

        @Test
        void signOut_noAuthentication_returns401() throws Exception {
                mockMvc.perform(get("/user/signout"))
                                .andExpect(status().isUnauthorized());
        }

        /*
         * =========================
         * REFRESH TOKEN
         * =========================
         */

        @Test
        void refreshToken_success() throws Exception {
                UUID userId = UUID.randomUUID();

                RestaurantUser user = new RestaurantUser();
                user.setId(userId);
                user.setEmail("test@mail.com");
                user.addRole("ROLE_USER");

                when(jwtTokenUtil.validateJwtRefreshToken("refresh-token"))
                                .thenReturn(true);

                when(jwtTokenUtil.getUserId("refresh-token"))
                                .thenReturn(userId);

                when(userRepository.findById(userId))
                                .thenReturn(Optional.of(user));

                when(jwtTokenUtil.generateAccessTokenFromUserId(
                                any(), any(), any()))
                                .thenReturn("new-access-token");

                mockMvc.perform(get("/user/refresh")
                                .cookie(new Cookie(
                                                "refreshToken", "refresh-token")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                                .andExpect(jsonPath("$.userId").value(userId.toString()));
        }

        @Test
        void refreshToken_missingCookie() throws Exception {
                mockMvc.perform(get("/user/refresh"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void refreshToken_invalidToken() throws Exception {
                when(jwtTokenUtil.validateJwtRefreshToken("bad")).thenReturn(false);

                mockMvc.perform(get("/user/refresh")
                                .cookie(new Cookie("refreshToken", "bad")))
                                .andExpect(status().isForbidden());
        }

        /*
         * =========================
         * UPDATE USER (ADMIN)
         * =========================
         */

        @Test
        @WithMockUser(username="admin@example.com", roles={"ADMIN"})
        void updateWaiter_success() throws Exception {
                RestaurantUser existing = new RestaurantUser();
                existing.setEmail("waiter@mail.com");
                existing.setPhoneNumber("111");
                existing.addRole("ROLE_WAITER");

                when(userRepository.findByEmail(anyString()))
                                .thenReturn(Optional.of(existing));

                mockMvc.perform(put("/user/update")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(existing)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("waiter@mail.com"))
                                .andExpect(jsonPath("$.roles[0]").value("ROLE_WAITER"));
        }
}
