package com.konstantinos.zito.webControllers;

import java.security.Principal;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.konstantinos.zito.model.dto.CreateOrderRequest;
import com.konstantinos.zito.model.dto.OrderResponse;
import com.konstantinos.zito.security.RestaurantUserDetails;
import com.konstantinos.zito.services.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private static final Logger logger = LoggerFactory.getLogger(Authentication.class);

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /*
     * Get a specific order as a guest
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable("orderId") UUID orderId,
            @RequestParam UUID cartId, @RequestParam(required = false) String guestEmail) {
                
            var order = orderService.getGuestOrder(orderId, cartId, Optional.ofNullable(guestEmail));
            if (order.isPresent()) {
                return ResponseEntity.ok().body(order.get());
            } else {
                return ResponseEntity.notFound().build();
            }
    }

    /*
     * Create a new order as a guest
     */
    @PostMapping()
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest order) {
        try {
            logger.info(String.format("Order received by %s with %d items", order.getGuestEmail(), order.getCartItems().size()));
            return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createGuestOrder(order));
        } catch (Exception e) {
            logger.error(String.format("Error creating order: %s", e.getMessage()));
            return ResponseEntity.badRequest().build();
        }
    }

    // ------- Waiter Endpoints ------- //

    /*
     * Get all orders for a specific waiter
     */
    @GetMapping("/waiter")
    @PreAuthorize("hasAnyRole('WAITER')")
    public ResponseEntity<List<OrderResponse>> getOrdersByWaiter(@AuthenticationPrincipal RestaurantUserDetails currentUser) {
        UUID waiterId = currentUser.getId();
        List<OrderResponse> orders = orderService.getUserOrder(waiterId);
        if(!orders.isEmpty()) {
           return ResponseEntity.ok().body(orders);
        } else {
            logger.info(String.format("No orders found for waiter %s", waiterId));
           return ResponseEntity.notFound().build();
        }   
    }

    /*
     * Create a new order for a waiter
     */
    @PostMapping("/waiter")
    @PreAuthorize("hasAnyRole('WAITER')")
    public ResponseEntity<OrderResponse> createOrderForWaiter(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest createOrderRequest) {

        var userDetails = (RestaurantUserDetails) authentication.getPrincipal();
        try {
            OrderResponse orderResponse = orderService.createOrder(userDetails.getId(), createOrderRequest);
            logger.info(String.format("Order %s created by %s with %d items", 
                    orderResponse.getId(),userDetails.getId(), createOrderRequest.getCartItems().size()));
            return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
        } catch (Exception e) {
            logger.error(String.format("Failed to create order for user %s", userDetails.getId()));
            return ResponseEntity.badRequest().build();
        }
    }

    /*
     * Update an order (Admin only)
     */
    @PutMapping("/waiter/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody CreateOrderRequest updatedOrderRequest,
            Authentication authentication) {

        String waiterEmail = authentication.getName();

        OrderResponse updatedOrder = orderService.updateOrder(orderId, waiterEmail, updatedOrderRequest);

        if (updatedOrder != null) {
            logger.info(String.format("Updating order %s updated by %s, created by %s" , orderId, waiterEmail, updatedOrder.getWaiterId()));
            return ResponseEntity.status(HttpStatus.CREATED).body(updatedOrder);
        } else {
            logger.error(String.format("Failed to update order for user %s", waiterEmail));
            return ResponseEntity.badRequest().build();
        }
    }

    /*
     * Delete an order (Admin only)
     */
    @DeleteMapping("/waiter/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable("orderId") UUID orderId,
            @AuthenticationPrincipal RestaurantUserDetails currentUser) {

        orderService.deleteOrder(orderId, currentUser.getId());
        logger.info(String.format("Delete order %s, deleted by %s", orderId, currentUser.getId()));
        return ResponseEntity.noContent().build();
    }

    /*
     * Get all orders for a table
     */
    @GetMapping("/order-point/{orderPointId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER')")
    public ResponseEntity<List<OrderResponse>> getOrdersByTable(@PathVariable("orderPointId") String     orderPointId) {
        List<OrderResponse> orders = orderService.getOrdersByTable(orderPointId);
        if (!orders.isEmpty()) {
            return ResponseEntity.ok().body(orders);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
