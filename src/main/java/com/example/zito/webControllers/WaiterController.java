package com.example.zito.webControllers;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.zito.model.Waiter;
import com.example.zito.repositories.WaiterRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/waiter")
public class WaiterController {

    private final WaiterRepository waiterRepository;

    public WaiterController(WaiterRepository waiterRepository) {
        this.waiterRepository = waiterRepository;
    }

    /*
     * Get all information about a specific waiter
     */
    @GetMapping("/{waiterName}")
    public ResponseEntity<Waiter> getWaiter(@PathVariable("waiterName") String name) {
        return ResponseEntity.ok(waiterRepository.findByName(name).get());
    }

    /**
     * Get all waiters.
     */
    @GetMapping
    public ResponseEntity<List<Waiter>> getAllWaiters() {
        List<Waiter> waiters = waiterRepository.findAll();
        return ResponseEntity.ok(waiters);
    }

    /**
     * Create a new waiter.
     */
    @PostMapping
    public ResponseEntity<Waiter> createWaiter(@Valid @RequestBody Waiter waiter) {
        Waiter savedWaiter = waiterRepository.save(waiter);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedWaiter);
    }

    /**
     * Update an existing waiter.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Waiter> updateWaiter(@PathVariable(value = "id") String id,
            @Valid @RequestBody Waiter waiterDetails) {
        Optional<Waiter> waiter = waiterRepository.findById(id);
        if (waiter.isPresent()) {

            waiter.get().setName(waiterDetails.getName());
            waiter.get().setPhoneNumber(waiterDetails.getPhoneNumber());

            final Waiter updatedWaiter = waiterRepository.save(waiter.get());

            return ResponseEntity.ok(updatedWaiter);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public void deleteWaiterById(@PathVariable("id") String id) {
        waiterRepository.deleteById(id);
    }

}
