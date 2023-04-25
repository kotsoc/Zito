package com.example.zito.webControllers;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.zito.model.RestaurantUser;
import com.example.zito.repositories.UserRepository;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/waiter")
public class RestaurantUserController {

    private final UserRepository waiterRepository;

    private final PasswordEncoder passwordEncoder;

    public RestaurantUserController(UserRepository waiterRepository, PasswordEncoder passwordEncoder) {
        this.waiterRepository = waiterRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /*
     * Get all information about a specific waiter
     */
    @GetMapping("/{waiterName}")
    public ResponseEntity<RestaurantUser> getWaiter(@PathVariable("waiterName") String name) {
        return ResponseEntity.ok(waiterRepository.findByUsername(name));
    }

    /**
     * Get all waiters.
     */
    @GetMapping
    public ResponseEntity<List<RestaurantUser>> getAllWaiters() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("Authori " + auth.getAuthorities().toString());
        List<RestaurantUser> waiters = waiterRepository.findAll();
        return ResponseEntity.ok(waiters);
    }

    /**
     * Create a new waiter.
     */
    @PostMapping
    public ResponseEntity<RestaurantUser> createWaiter(@Valid @RequestBody RestaurantUser waiter) {
        RestaurantUser newUser = new RestaurantUser();
        newUser.setUsername(waiter.getUsername());
        newUser.setPassword(passwordEncoder.encode(waiter.getPassword()));
        newUser.setPhoneNumber(waiter.getPhoneNumber());
        waiterRepository.save(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(waiter);
    }

    /**
     * Update an existing waiter.
     */
    @PutMapping
    public ResponseEntity<RestaurantUser> updateWaiter(@Valid @RequestBody RestaurantUser waiterDetails) {
        Optional<RestaurantUser> waiter = waiterRepository.findById(waiterDetails.getId());
        if (waiter.isPresent()) {
            final RestaurantUser updatedWaiter = waiter.get();
            waiter.get().setUsername(waiterDetails.getUsername());
            waiter.get().setPhoneNumber(waiterDetails.getPhoneNumber());
            waiter.get().setPassword(passwordEncoder.encode(waiterDetails.getPassword()));
            waiter.get().setRoles(waiterDetails.getRoles());

            waiterRepository.save(waiter.get());

            return ResponseEntity.ok(updatedWaiter);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @RolesAllowed("admin")
    @DeleteMapping("/{id}")
    public void deleteWaiterById(@PathVariable("id") String id) {
        waiterRepository.deleteById(id);
    }

}
