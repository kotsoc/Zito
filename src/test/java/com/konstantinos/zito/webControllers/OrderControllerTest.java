package com.konstantinos.zito.webControllers;

import com.konstantinos.zito.model.OrderStatus;
import com.konstantinos.zito.model.dto.CartItemRequest;
import com.konstantinos.zito.model.dto.CreateOrderRequest;
import com.konstantinos.zito.model.dto.OrderResponse;
import com.konstantinos.zito.security.RestaurantUserDetails;
import com.konstantinos.zito.services.OrderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;
        
    @Mock
    private CartItemRequest cartItemRequest;

    private RestaurantUserDetails currentUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        currentUser = new RestaurantUserDetails(
                UUID.randomUUID(),
                "waiter@example.com",
                List.of(new SimpleGrantedAuthority("ROLE_WAITER")));

    }

    @Test
    void getOrder_found_returnsOrder() {
        UUID orderId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        String guestEmail = "guest@example.com";
        OrderResponse orderResponse = new OrderResponse();
        when(orderService.getGuestOrder(eq(orderId), eq(cartId), eq(Optional.of(guestEmail))))
                .thenReturn(Optional.of(orderResponse));

        ResponseEntity<OrderResponse> response = orderController.getOrder(orderId, cartId, guestEmail);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(orderResponse, response.getBody());
    }

    @Test
    void getOrder_notFound_returnsNotFound() {
        UUID orderId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        String guestEmail = "guest@example.com";
        when(orderService.getGuestOrder(eq(orderId), eq(cartId), eq(Optional.of(guestEmail))))
                .thenReturn(Optional.empty());

        ResponseEntity<OrderResponse> response = orderController.getOrder(orderId, cartId, guestEmail);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void createOrder_success_returnsCreated() {
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(request.getCartItems()).thenReturn(List.of(cartItemRequest));
        OrderResponse orderResponse = new OrderResponse();
        when(orderService.createGuestOrder(request)).thenReturn(orderResponse);

        ResponseEntity<OrderResponse> response = orderController.createOrder(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(orderResponse, response.getBody());
    }

    @Test
    void createOrder_exception_returnsBadRequest() {
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(request.getCartItems()).thenReturn(List.of(cartItemRequest));
        when(orderService.createGuestOrder(request)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<OrderResponse> response = orderController.createOrder(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void updateOrderStatus_success_returnsOk() {
        UUID orderId = UUID.randomUUID();
        OrderStatus status = OrderStatus.CONFIRMED;
        OrderResponse updatedOrder = new OrderResponse();
        when(orderService.updateOrderStatus(orderId, status)).thenReturn(updatedOrder);

        ResponseEntity<OrderResponse> response = orderController.updateOrderStatus(orderId, status);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedOrder, response.getBody());
    }

    @Test
    void updateOrderStatus_exception_returnsBadRequest() {
        UUID orderId = UUID.randomUUID();
        OrderStatus status = OrderStatus.CONFIRMED;
        when(orderService.updateOrderStatus(orderId, status)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<OrderResponse> response = orderController.updateOrderStatus(orderId, status);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getOrdersByWaiter_found_returnsOrders() {
        UUID waiterId = UUID.randomUUID();
        List<OrderResponse> orders = List.of(new OrderResponse(), new OrderResponse());
        when(orderService.getUserOrder(currentUser.getId())).thenReturn(orders);

        ResponseEntity<List<OrderResponse>> response = orderController.getOrdersByWaiter(currentUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(orders, response.getBody());
    }

    @Test
    void getOrdersByWaiter_notFound_returnsNotFound() {
        String waiterEmail = "waiter@example.com";
        UUID waiterId = UUID.randomUUID();
        when(orderService.getUserOrder(waiterId)).thenReturn(Collections.emptyList());

        ResponseEntity<List<OrderResponse>> response = orderController.getOrdersByWaiter(currentUser);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void createOrderForWaiter_success_returnsCreated() {
        UUID waiterId = UUID.randomUUID();
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(request.getCartItems()).thenReturn(List.of(cartItemRequest));
        OrderResponse orderResponse = mock(OrderResponse.class);
        when(orderResponse.getId()).thenReturn(waiterId);
        when(orderService.createOrder(currentUser.getId(), request)).thenReturn(orderResponse);

        ResponseEntity<OrderResponse> response = orderController.createOrderForWaiter(currentUser, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(orderResponse, response.getBody());
    }

    @Test
    void createOrderForWaiter_exception_returnsBadRequest() {
        UUID waiterId = UUID.randomUUID();
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(request.getCartItems()).thenReturn(List.of(cartItemRequest));
    
        when(orderService.createOrder(waiterId, request)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<OrderResponse> response = orderController.createOrderForWaiter(currentUser, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void updateOrder_success_returnsCreated() {
        UUID orderId = UUID.randomUUID();
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        OrderResponse updatedOrder = mock(OrderResponse.class);
        when(updatedOrder.getWaiterId()).thenReturn(currentUser.getId());
        when(orderService.updateOrder(orderId, currentUser.getId(), request)).thenReturn(updatedOrder);

        ResponseEntity<OrderResponse> response = orderController.updateOrder(orderId, request, currentUser);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(updatedOrder, response.getBody());
    }

    @Test
    void updateOrder_null_returnsBadRequest() {
        UUID orderId = UUID.randomUUID();
        String waiterEmail = "admin@example.com";
        UUID waiterId = UUID.randomUUID();
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(orderService.updateOrder(orderId, waiterId, request)).thenReturn(null);

        ResponseEntity<OrderResponse> response = orderController.updateOrder(orderId, request, currentUser);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void deleteOrder_success_returnsNoContent() {
        UUID orderId = UUID.randomUUID();

        ResponseEntity<Void> response = orderController.deleteOrder(orderId, currentUser);

        verify(orderService).deleteOrder(orderId, currentUser.getId());
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void getOrdersByTable_found_returnsOrders() {
        String orderPointId = "table-1";
        List<OrderResponse> orders = List.of(new OrderResponse());
        when(orderService.getOrdersByTable(orderPointId)).thenReturn(orders);

        ResponseEntity<List<OrderResponse>> response = orderController.getOrdersByTable(orderPointId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(orders, response.getBody());
    }

    @Test
    void getOrdersByTable_notFound_returnsNotFound() {
        String orderPointId = "table-1";
        when(orderService.getOrdersByTable(orderPointId)).thenReturn(Collections.emptyList());

        ResponseEntity<List<OrderResponse>> response = orderController.getOrdersByTable(orderPointId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }
}