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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.konstantinos.zito.model.OrderStatus;
import com.konstantinos.zito.model.dto.CreateOrderRequest;
import com.konstantinos.zito.model.dto.OrderResponse;
import com.konstantinos.zito.model.dto.OrderStatusRequest;
import com.konstantinos.zito.model.dto.CancelOrderRequest;
import org.springframework.web.server.ResponseStatusException;
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
            @RequestHeader(value = "X-Order-Credential", required = false) String credential) {
                
            var order = orderService.getGuestOrder(orderId, credential);
            if (order.isPresent()) {
                return ResponseEntity.ok().body(order.get());
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
            }
    }

    /*
     * Create a new order as a guest
     */
    @PostMapping()
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest order) {
            logger.info(String.format("Order received by %s with %d items", order.getGuestEmail(), order.getCartItems().size()));
            return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createGuestOrder(order));
    }

    /*
     * Update order status (Waiter/Admin only)
     */
    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN', 'MANAGER') or (hasRole('KITCHEN') and (#status.status.name() == 'PREPARING' or #status.status.name() == 'READY'))")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody OrderStatusRequest status) {

            OrderResponse updatedOrder = orderService.updateOrderStatus(orderId, status.getStatus(), status.getVersion());
            logger.info(String.format("Order %s status updated to %s", orderId, status.getStatus()));
            return ResponseEntity.ok().body(updatedOrder);
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
           throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Orders not found");
        }   
    }

    /*
     * Create a new order for a waiter
     */
    @PostMapping("/waiter")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> createOrderForWaiter(
           @AuthenticationPrincipal RestaurantUserDetails currentUser,
            @Valid @RequestBody CreateOrderRequest createOrderRequest) {

            OrderResponse orderResponse = orderService.createOrder(currentUser.getId(), createOrderRequest);
            logger.info(String.format("Order %s created by %s with %d items", 
                    orderResponse.getId(),currentUser.getId(), createOrderRequest.getCartItems().size()));
            return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
    }

    /*
     * Update an order (Admin only)
     */
    @PutMapping("/waiter/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable("orderId") UUID orderId,
            @Valid @RequestBody CreateOrderRequest updatedOrderRequest,
           @AuthenticationPrincipal RestaurantUserDetails currentUser) {

        OrderResponse updatedOrder = orderService.updateOrder(orderId, currentUser.getId(), updatedOrderRequest);

        if (updatedOrder != null) {
            logger.info(String.format("Updating order %s updated by %s, created by %s" , orderId, currentUser.getId(), updatedOrder.getWaiterId()));
            return ResponseEntity.status(HttpStatus.CREATED).body(updatedOrder);
        } else {
            logger.error(String.format("Failed to update order for user %s", currentUser.getId()));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order update rejected");
        }
    }

    /*
     * Delete an order (Admin only)
     */
    @DeleteMapping("/waiter/{orderId}")
    @Deprecated
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable("orderId") UUID orderId,
            @RequestParam Long version, @RequestParam String reason,
            @AuthenticationPrincipal RestaurantUserDetails currentUser) {

        orderService.cancelOrder(orderId, currentUser.getId(), version, reason);
        logger.info(String.format("Delete order %s, deleted by %s", orderId, currentUser.getId()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{orderId}/cancellation")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID orderId,
            @Valid @RequestBody CancelOrderRequest request,
            @AuthenticationPrincipal RestaurantUserDetails currentUser) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId, currentUser.getId(), request.getVersion(), request.getReason()));
    }

    /*
     * Get all orders for a table
     */
    @GetMapping("/order-point/{orderPointId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'WAITER', 'KITCHEN')")
    public ResponseEntity<List<OrderResponse>> getOrdersByTable(@PathVariable("orderPointId") String     orderPointId) {
        List<OrderResponse> orders = orderService.getOrdersByTable(orderPointId);
        if (!orders.isEmpty()) {
            return ResponseEntity.ok().body(orders);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Orders not found");
        }
    }

       /*
     * Get all orders 
     */
    @GetMapping("")
    @PreAuthorize("hasAnyRole('ADMIN', 'WAITER', 'MANAGER', 'KITCHEN')")
    public ResponseEntity<List<OrderResponse>> getOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok().body(orders);
    }

}
