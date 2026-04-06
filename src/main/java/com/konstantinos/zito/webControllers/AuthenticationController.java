package com.konstantinos.zito.webControllers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.konstantinos.zito.model.RestaurantUser;
import com.konstantinos.zito.model.dto.UpdateUserRequest;
import com.konstantinos.zito.model.dto.LoginRequest;
import com.konstantinos.zito.model.dto.LoginResponse;
import com.konstantinos.zito.model.dto.RefreshResponse;
import com.konstantinos.zito.model.dto.RegisterRequest;
import com.konstantinos.zito.repositories.UserRepository;
import com.konstantinos.zito.security.JwtTokenUtil;
import com.konstantinos.zito.security.RestaurantUserDetails;
import com.konstantinos.zito.services.TokenInvalidatorService;

import jakarta.validation.Valid;

// TODO: Separate Authentication and User Management Controllers -- Make it more RESTful
/*
POST   /auth/login
POST   /auth/signup
GET    /auth/refresh
POST   /auth/logout
----
PUT    /users/{id}
DELETE /users/{id}

*/



@RestController
@Profile("!dev")
@RequestMapping("/user")
public class AuthenticationController {
        private static final Logger logger = LoggerFactory.getLogger(Authentication.class);
        final UserRepository userRepository;
        final AuthenticationManager authenticationManager;

        final JwtTokenUtil jwtTokenUtil;
        private PasswordEncoder passwordEncoder;
        final TokenInvalidatorService tokenInvalidatorService;

        public AuthenticationController(UserRepository userRepository, AuthenticationManager authenticationManager,
                        JwtTokenUtil jwtTokenUtil, PasswordEncoder passwordEncoder,
                        TokenInvalidatorService tokenInvalidatorService) {
                this.userRepository = userRepository;
                this.authenticationManager = authenticationManager;
                this.jwtTokenUtil = jwtTokenUtil;
                this.passwordEncoder = passwordEncoder;
                this.tokenInvalidatorService = tokenInvalidatorService;
        }

        /**
         * Sign in with an existing account
         * 
         * @param request {@code LoginRequest} with a username and a password
         * 
         * @Returns an jwt token to be used in future requets
         */

        @PostMapping("/signin")
        public ResponseEntity<LoginResponse> signIn(@Valid @RequestBody LoginRequest request) {

                Authentication authentication = authenticationManager
                                .authenticate(new UsernamePasswordAuthenticationToken(
                                                request.getEmail(),
                                                request.getPassword()));

                if (!(authentication.getPrincipal() instanceof RestaurantUserDetails userDetails)) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                List<String> roles = userDetails.getAuthorities().stream()
                                .map(item -> item.getAuthority())
                                .toList();

                String accessToken = jwtTokenUtil.generateAccessToken(userDetails);

                ResponseCookie jwtCookie = jwtTokenUtil.generateRefreshJwtCookie(userDetails);

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                        .body(new LoginResponse(accessToken,
                                                userDetails.getUsername(),
                                                roles));
        }

        /**
         * Sign out an existing account, the access token will be blacklisted
         * 
         */

        @GetMapping("/signout")
        public ResponseEntity<Void> signOut() {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null || !(authentication.getCredentials() instanceof String)) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                
                this.tokenInvalidatorService.invalidateToken(authentication.getCredentials().toString());
                // Clear SecurityContext
                SecurityContextHolder.clearContext();

                // Create cookie to clear the refresh token
                ResponseCookie refreshCookie = jwtTokenUtil.generateClearRefreshCookie();
                logger.info("user signed out!");
                return ResponseEntity.status(HttpStatus.OK)
                                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString()).build();
        }

        /**
         * 
         * Creates a new user by persisting the given {@code RestaurantUser}
         * 
         * @param user The {@code RestaurantUser} object to be persisted.
         * @return A {@code ResponseEntity} with a status of {@code HttpStatus.CREATED}
         *         and the created waiter in the body.
         * @throws javax.validation.ValidationException
         */
        @PostMapping("/signup")
        public ResponseEntity<RestaurantUser> createRestaurauntUser(@Valid @RequestBody RegisterRequest userRequest) {
                RestaurantUser newUser = new RestaurantUser();
                newUser.setEmail(userRequest.getEmail());
                newUser.setId(java.util.UUID.randomUUID());
                newUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
                newUser.setPhoneNumber(userRequest.getPhoneNumber());
                newUser.addRole("ROLE_GUEST");
                try {
                        userRepository.save(newUser);
                        logger.info("User {} created sucessfully", newUser.getEmail());
                        return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
                } catch (Exception e) {
                        logger.warn("Could not create User {} : duplicate Username", newUser.getEmail());
                        return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
                }
        }

        /**
         * 
         * Refresh the token for a certain {@code RestaurantUser},
         * 
         * @return A {@code RefreshResponse} containing a token for the user
         */
        @PostMapping("/refresh")
        public ResponseEntity<?> refreshToken(
                        @CookieValue(value = "refreshToken", required = false) String refreshToken) {
               
                if (refreshToken == null || !jwtTokenUtil.validateJwtRefreshToken(refreshToken)) {
                        return ResponseEntity.status(401).body(Map.of("error", "Refresh token invalid or missing"));
                }

                UUID userId = jwtTokenUtil.getUserId(refreshToken);

                var user = userRepository.findById(userId);
                //TODO: Refresh token persist on DB and Revoke on logout --- @tokenInvalidationService/extend with DB
                if (user.isEmpty()) {

                        return ResponseEntity.status(404).body(Map.of("error", "User not found"));
                }
                String newAccessToken = jwtTokenUtil.generateAccessTokenFromUserId(
                                userId,
                                user.get().getEmail(),
                                user.get().getRoles().stream().toList());
                return ResponseEntity.status(HttpStatus.OK).body(new RefreshResponse(newAccessToken, userId));
        }

        /**
         * 
         * Get an access token for a certain {@code RestaurantUser},
         * 
         * @return A {@code Restaurant User} and an access token for the user
         */
        @GetMapping("/me")
        public ResponseEntity<RestaurantUser> me(@AuthenticationPrincipal RestaurantUserDetails currentUser) {
                var user = userRepository.findById(currentUser.getId());
                if (user.isPresent()) {
                        return ResponseEntity.ok(user.get());
                } else {
                        return ResponseEntity.notFound().build();
                }
        }

        /////----------  User Management Endpoints ------------/////

        /**
         * 
         * Get all users (Admin only)
         * 
         * @return A {@code ResponseEntity} containing a list of all RestaurantUsers
         */
        @GetMapping("/all")
        @PreAuthorize("hasRole('ROLE_ADMIN')")
        public ResponseEntity<List<RestaurantUser>> getAllUsers() {
                List<RestaurantUser> users = userRepository.findAll();
                return ResponseEntity.ok(users);
        }

        /**
         * 
         * Attempts to delete a user if it exists
         * 
         * @param user The {@code RestaurantUser} object to be persisted.
         * @return A {@code ResponseEntity} with a status of the opeartion
         */
        @DeleteMapping("/{username}")
        @PreAuthorize("hasRole('ADMIN')")
        public void deleteWaiterById(@PathVariable("username") String username) {
                logger.info("Deleting user {}", username);
                var user = userRepository.findByEmail(username);
                if (user.isPresent()) {
                        userRepository.deleteById(user.get().getId());
                }
        }

        /**
         * 
         * Update an existing user by persisting the given {@code RestaurantUser},
         * updating is the only way of changing/adding the role of a user
         * 
         * @param user The {@code RestaurantUser} object to be persisted.
         * @return A {@code ResponseEntity} with a status of the opeartion
         * @throws javax.validation.ValidationException
         */
        @PutMapping("/update")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<RestaurantUser> updateWaiter(@Valid @RequestBody UpdateUserRequest updatedUser) {
                Optional<RestaurantUser> existingUser = userRepository.findByEmail(updatedUser.getEmail());
                if (existingUser.isPresent()) {
                        final RestaurantUser updatedWaiter = existingUser.get();
                        existingUser.get().setEmail(updatedUser.getEmail());
                        existingUser.get().setPhoneNumber(updatedUser.getPhoneNumber());
                        existingUser.get().setRoles(updatedUser.getRoles());

                        userRepository.save(existingUser.get());
                        logger.info("User {} updated sucessfully", updatedUser.getEmail());
                        return ResponseEntity.ok(updatedWaiter);
                } else {
                        logger.warn("User {} was not found", updatedUser.getEmail());
                        return ResponseEntity.notFound().build();
                }
        }
}
