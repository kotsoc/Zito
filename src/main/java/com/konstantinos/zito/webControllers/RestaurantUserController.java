package com.konstantinos.zito.webControllers;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.konstantinos.zito.model.RestaurantUser;
import com.konstantinos.zito.repositories.UserRepository;

@RestController
@RequestMapping("/api/v1/waiter")
public class RestaurantUserController {

    private final UserRepository waiterRepository;

    public RestaurantUserController(UserRepository waiterRepository, PasswordEncoder passwordEncoder) {
        this.waiterRepository = waiterRepository;
    }

    /*
     * Get all information about a specific waiter
     */
    @GetMapping("/{waiterName}")
    public ResponseEntity<RestaurantUser> getWaiter(@PathVariable("waiterName") String name) {
        var maybeWaiter = waiterRepository.findByUsername(name);
        if (maybeWaiter.isPresent())
            return ResponseEntity.ok(maybeWaiter.get());
        else
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + name + " not found");
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

}
