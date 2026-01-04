package com.konstantinos.zito.webControllers;

import java.io.IOException;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.konstantinos.zito.model.AuthProvider;
import com.konstantinos.zito.model.RestaurantUser;
import com.konstantinos.zito.model.dto.LoginResponse;
import com.konstantinos.zito.repositories.UserRepository;
import com.konstantinos.zito.security.JwtTokenUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private static final String DEFAULT_ROLE = "ROLE_GUEST";

    public OAuth2SuccessHandler(UserRepository userRepository, JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String providerId = oAuth2User.getAttribute("sub");

        // Load existing user or create new Google user
        RestaurantUser user = userRepository.findByEmail(email)
                .map(existingUser -> linkGoogleIfLocal(existingUser, providerId))
                .orElseGet(() -> createGoogleUser(email, providerId));

        // Ensure roles exist
        if (user.getRoles().isEmpty()) {
            user.addRole(DEFAULT_ROLE);
            userRepository.save(user);
        }

        // Generate access token and refresh token cookie
        String accessToken = jwtTokenUtil.generateAccessToken(user);
        ResponseCookie refreshCookie = jwtTokenUtil.generateRefreshJwtCookie(user);

        // Build login response
        LoginResponse loginResponse = new LoginResponse(
                accessToken,
                user.getEmail(),
                user.getRoles().stream().toList()
        );

        // Return JSON response with refresh cookie header
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(loginResponse));
    }

    private RestaurantUser linkGoogleIfLocal(RestaurantUser existingUser, String providerId) {
        if (existingUser.getProvider() == AuthProvider.LOCAL) {
            existingUser.setProvider(AuthProvider.GOOGLE);
            existingUser.setProviderId(providerId);
            userRepository.save(existingUser);
        }
        return existingUser;
    }

    private RestaurantUser createGoogleUser(String email, String providerId) {
        RestaurantUser user = new RestaurantUser();
        user.setEmail(email);
        user.setProvider(AuthProvider.GOOGLE);
        user.setProviderId(providerId);
        user.setRoles(Set.of(DEFAULT_ROLE));
        return userRepository.save(user);
    }
}
