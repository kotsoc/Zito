package com.konstantinos.zito.webControllers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.konstantinos.zito.model.LoginRequest;
import com.konstantinos.zito.model.LoginResponse;
import com.konstantinos.zito.model.RestaurantUser;
import com.konstantinos.zito.repositories.UserRepository;
import com.konstantinos.zito.security.JwtTokenUtil;

import jakarta.validation.Valid;

@RestController
@Profile("!dev")
@RequestMapping("/user")
public class AuthenticationController {
        private static final Logger logger = LoggerFactory.getLogger(Authentication.class);
        final UserRepository userRepository;
        final AuthenticationManager authenticationManager;

        final JwtTokenUtil jwtTokenUtil;
        private PasswordEncoder passwordEncoder;

        public AuthenticationController(UserRepository userRepository, AuthenticationManager authenticationManager,
                        JwtTokenUtil jwtTokenUtil, PasswordEncoder passwordEncoder) {
                this.userRepository = userRepository;
                this.authenticationManager = authenticationManager;
                this.jwtTokenUtil = jwtTokenUtil;
                this.passwordEncoder = passwordEncoder;
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
                                                roles));
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
        @PostMapping("/register")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<RestaurantUser> createRestaurauntUser(@Valid @RequestBody RestaurantUser user) {
                RestaurantUser newUser = new RestaurantUser();
                newUser.setUsername(user.getUsername());
                newUser.setPassword(passwordEncoder.encode(user.getPassword()));
                newUser.setPhoneNumber(user.getPhoneNumber());
                newUser.addRole("ROLE_WAITER");
                userRepository.save(newUser);
                logger.info("User {} created sucessfully", newUser.getUsername());
                return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        }

        /**
         * 
         * Update an existing user by persisting the given {@code RestaurantUser},
         * updating is the only way of changing the role of a user
         * 
         * @param user The {@code RestaurantUser} object to be persisted.
         * @return A {@code ResponseEntity} with a status of the opeartion
         * @throws javax.validation.ValidationException
         */
        @PutMapping("/update")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<RestaurantUser> updateWaiter(@Valid @RequestBody RestaurantUser updatedUser) {
                Optional<RestaurantUser> existingUser = userRepository.findById(updatedUser.getId());
                if (existingUser.isPresent()) {
                        final RestaurantUser updatedWaiter = existingUser.get();
                        existingUser.get().setUsername(updatedUser.getUsername());
                        existingUser.get().setPhoneNumber(updatedUser.getPhoneNumber());
                        existingUser.get().setPassword(passwordEncoder.encode(updatedUser.getPassword()));
                        existingUser.get().setRoles(updatedUser.getRoles());

                        userRepository.save(existingUser.get());
                        logger.info("User {} updated sucessfully", updatedUser.getUsername());
                        return ResponseEntity.ok(updatedWaiter);
                } else {
                        logger.warn("User {} was not found", updatedUser.getUsername());
                        return ResponseEntity.notFound().build();
                }
        }

        @DeleteMapping("/{username}")
        @PreAuthorize("hasRole('ADMIN')")
        public void deleteWaiterById(@PathVariable("username") String username) {
                logger.info("Deleting user {}", username);
                userRepository.deleteById(username);
        }
}
