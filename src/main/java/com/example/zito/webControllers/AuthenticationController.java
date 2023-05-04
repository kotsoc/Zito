package com.example.zito.webControllers;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.zito.model.LoginRequest;
import com.example.zito.model.LoginResponse;
import com.example.zito.repositories.UserRepository;
import com.example.zito.security.JwtTokenUtil;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
        private static final Logger logger = LoggerFactory.getLogger(Authentication.class);

        final UserRepository userRepository;

        final AuthenticationManager authenticationManager;

        final JwtTokenUtil jwtTokenUtil;

        public AuthenticationController(UserRepository userRepository, AuthenticationManager authenticationManager,
                        JwtTokenUtil jwtTokenUtil) {
                this.userRepository = userRepository;
                this.authenticationManager = authenticationManager;
                this.jwtTokenUtil = jwtTokenUtil;
        }

        /*
         * Create a account
         */
        @PostMapping("/signin")
        public ResponseEntity<LoginResponse> signIn(@Valid @RequestBody LoginRequest request) {

                Authentication authentication = authenticationManager
                                .authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(),
                                                request.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                ResponseCookie jwtCookie = jwtTokenUtil.generateJwtCookie(userDetails);

                List<String> roles = userDetails.getAuthorities().stream()
                                .map(item -> item.getAuthority())
                                .collect(Collectors.toList());
                // authentication.setDetails(new
                // WebAuthenticationDetailsSource().buildDetails(request));
                return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                                .body(new LoginResponse(jwtCookie.toString(),
                                                userDetails.getUsername(),
                                                (Collection<SimpleGrantedAuthority>) userDetails.getAuthorities()));
        }

}
