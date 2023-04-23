package com.example.zito.webControllers;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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
import com.example.zito.repositories.TableRepository;
import com.example.zito.repositories.WaiterRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/order")
public class OrderController {

    // private OrderService orderService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    WaiterRepository waiterRepository;

    @Autowired
    TableRepository tableRepository;

    /*
     * Get all orders for a specific waiter
     */
    @GetMapping("/{waiterName}")
    public ResponseEntity<List<Order>> getOrdersByWaiter(@PathVariable("waiterName") String waiterName) {
        Optional<Waiter> waiter = waiterRepository.findByName(waiterName);
        if (waiter.isPresent()) {
            List<Order> orders = orderRepository.findByWaiter(waiter.get()).stream()
                    .sorted(Comparator.comparing(Order::getTable)).collect(Collectors.toList());
            return ResponseEntity.ok().body(orders);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /*
     * Get all orders for a table
     */
    @GetMapping("/table/{tableId}")
    public ResponseEntity<List<Order>> getOrdersByTable(@PathVariable("tableId") int tableId) {
        List<Order> orders = orderRepository.findByTable(tableId);
        if (!orders.isEmpty()) {
            return ResponseEntity.ok().body(orders);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /*
     * Create a new order, need to correspond to a valid waiter
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@PathVariable("waiterId") String waiterId,
            @Valid @RequestBody Order order) {
        if (waiterRepository.findById(waiterId).isPresent()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(orderRepository.save(order));
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    /*
     * Update an order
     */
    @PutMapping("/{waiterId}/{tableId}")
    public ResponseEntity<Order> UpdateOrder(@PathVariable("waiterId") String waiterId,
            @PathVariable("tableId") String tableId, @Valid @RequestBody Order updatedOrder) {
        if (tableRepository.existsById(tableId) && waiterRepository.existsById(waiterId)) {
            return ResponseEntity.ok(orderRepository.save(updatedOrder));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /*
     * Deletes an order, need to correspond to a valid waiter/table
     */
    @DeleteMapping("/{waiterId}/{tableId}")
    public ResponseEntity<Order> deleteTable(@PathVariable("waiterId") String waiterId,
            @PathVariable("tableId") String tableId) {
        if (waiterRepository.findById(waiterId).isPresent() && tableRepository.findById(tableId).isPresent()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
