package com.konstantinos.zito.webControllers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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

import com.konstantinos.zito.model.Order;
import com.konstantinos.zito.model.RestaurantUser;
import com.konstantinos.zito.repositories.ChangedOrderRepository;
import com.konstantinos.zito.repositories.OrderRepository;
import com.konstantinos.zito.repositories.TableRepository;
import com.konstantinos.zito.repositories.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/order")
public class OrderController {

    private final OrderRepository orderRepository;
    private final UserRepository waiterRepository;
    private final TableRepository tableRepository;
    private final ChangedOrderRepository changedOrderRepository;

    public OrderController(OrderRepository orderRepository, UserRepository waiterRepository,
            TableRepository tableRepository, ChangedOrderRepository changedOrderRepository) {
        this.orderRepository = orderRepository;
        this.waiterRepository = waiterRepository;
        this.tableRepository = tableRepository;
        this.changedOrderRepository = changedOrderRepository;
    }

    /*
     * Get all orders for a specific waiter
     */
    @GetMapping("/{waiterName}")
    public ResponseEntity<List<Order>> getOrdersByWaiter(@PathVariable("waiterName") String waiterName) {
        var waiter = waiterRepository.findByUsername(waiterName);
        if (waiter.isPresent()) {
            List<Order> orders = orderRepository.findByWaiterName(waiter.get().getUsername()).stream()
                    .sorted(Comparator.comparing(Order::getTableNumber)).collect(Collectors.toList());
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
        List<Order> orders = orderRepository.findByTableNumber(tableId);
        if (!orders.isEmpty()) {
            return ResponseEntity.ok().body(orders);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /*
     * Create a new order, need to correspond to a valid waiter
     */
    @PostMapping("/{waiterName}")
    public ResponseEntity<Order> createOrder(@PathVariable("waiterName") String waiterName,
            @Valid @RequestBody Order order) {
        var waiter = waiterRepository.findByUsername(waiterName);
        if (waiter.isPresent()) {
            order.setWaiterName(waiterName);
            var date = new Date();
            order.setOrder_time(date);
            order.setLatest_update(date);
            return ResponseEntity.status(HttpStatus.CREATED).body(orderRepository.save(order));
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    /*
     * Update an order
     */
    @PutMapping("/{waiter}/{orderId}")
    public ResponseEntity<Order> UpdateOrder(@PathVariable("waiterId") String waiterName,
            @PathVariable("orderId") String orderId, @Valid @RequestBody Order updatedOrder) {
        var oldOrder = orderRepository.findById(orderId);

        if (oldOrder.isPresent()
                && tableRepository.findById(updatedOrder.getTableNumber()).isPresent()
                && waiterRepository.existsByUsername(waiterName)) {
            // Save the old order reference
            changedOrderRepository.save(oldOrder.get());
            var newOrder = oldOrder.get();
            newOrder.setItems(updatedOrder.getItems());
            newOrder.setLatest_update(new Date());
            newOrder.setPaid(updatedOrder.isPaid());
            newOrder.setStatus(updatedOrder.getStatus());
            newOrder.setTableNumber(updatedOrder.getTableNumber());
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
