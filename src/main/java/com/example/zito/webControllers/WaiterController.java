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
        return waiterRepository.findById(name);
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

    // @GetMapping("/my-documents/{id}")
    // public MyDocument getMyDocumentById(@PathVariable("id") String id) {
    // return myRepository.findById(id).orElse(null);
    // }

    // @PutMapping("/my-documents/{id}")
    // public MyDocument updateMyDocument(@PathVariable("id") String id, @Valid
    // @RequestBody MyDocument myDocument) {
    // myDocument.setId(id);
    // return myRepository.save(myDocument);
    // }

    // @DeleteMapping("/my-documents/{id}")
    // public void deleteMyDocumentById(@PathVariable("id") String id) {
    // myRepository.deleteById(id);
    // }

}
