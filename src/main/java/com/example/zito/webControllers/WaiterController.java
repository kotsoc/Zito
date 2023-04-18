package com.example.zito.webControllers;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.zito.model.Order;
import com.example.zito.model.Table;
import com.example.zito.model.Waiter;
import com.example.zito.repositories.OrderRepository;
import com.example.zito.repositories.WaiterRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
public class WaiterController {

    // private OrderService orderService;

    @Autowired
    WaiterRepository waiterRepository;

    /*
     * Get all information about a specific waiter
     */
    @GetMapping("/waiter/{waiterName}")
    public Optional<Waiter> getWaiter(@PathVariable("waiterName") String name) {
        return waiterRepository.findByName(name);
    }

    /*
     * Get all waiters
     */
    @GetMapping("/waiter/all")
    public List<Waiter> getAllWaiters() {
        return waiterRepository.findAll();
    }

    @PostMapping("/waiter")
    public Waiter createWaiter(@Valid @RequestBody Waiter waiter) {
        return waiterRepository.save(waiter);
    }

    @PutMapping("/waiter")
    public Waiter updateWaiter(@Valid @RequestBody Waiter waiter) {
        return waiterRepository.save(waiter);
    }

    @DeleteMapping("/waiter/{id}")
    public void deleteWaiterById(@PathVariable("id") String id) {
        waiterRepository.deleteById(id);
    }

}
