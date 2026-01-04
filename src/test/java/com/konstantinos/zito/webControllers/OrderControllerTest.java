package com.konstantinos.zito.webControllers;

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
import org.springframework.security.core.Authentication;
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
    private RestaurantUserDetails currentUser;

    @Mock
    private Authentication authentication;

    @Mock
    private CartItemRequest cartItemRequest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

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
    void getOrdersByWaiter_found_returnsOrders() {
        String waiterEmail = "waiter@example.com";
        UUID waiterId = UUID.fromString("waiter-uuid-string");
        List<OrderResponse> orders = List.of(new OrderResponse(), new OrderResponse());
        when(currentUser.getId()).thenReturn(waiterId);
        when(orderService.getUserOrder(waiterId)).thenReturn(orders);

        ResponseEntity<List<OrderResponse>> response = orderController.getOrdersByWaiter(currentUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(orders, response.getBody());
    }

    @Test
    void getOrdersByWaiter_notFound_returnsNotFound() {
        String waiterEmail = "waiter@example.com";
        UUID waiterId = UUID.fromString("waiter-uuid-string");
        when(authentication.getName()).thenReturn(waiterEmail);
        when(orderService.getUserOrder(waiterId)).thenReturn(Collections.emptyList());

        ResponseEntity<List<OrderResponse>> response = orderController.getOrdersByWaiter(currentUser);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void createOrderForWaiter_success_returnsCreated() {
        String waiterEmail = "waiter@example.com";
        UUID waiterId = UUID.fromString("waiter-uuid-string");
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(request.getCartItems()).thenReturn(List.of(cartItemRequest));
        OrderResponse orderResponse = mock(OrderResponse.class);
        when(orderResponse.getId()).thenReturn(UUID.randomUUID());
        when(authentication.getName()).thenReturn(waiterEmail);
        when(orderService.createOrder(waiterId, request)).thenReturn(orderResponse);

        ResponseEntity<OrderResponse> response = orderController.createOrderForWaiter(authentication, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(orderResponse, response.getBody());
    }

    @Test
    void createOrderForWaiter_exception_returnsBadRequest() {
        String waiterEmail = "waiter@example.com";
        UUID waiterId = UUID.fromString("waiter-uuid-string");
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(request.getCartItems()).thenReturn(List.of(cartItemRequest));
        when(authentication.getName()).thenReturn(waiterEmail);
        when(orderService.createOrder(waiterId, request)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<OrderResponse> response = orderController.createOrderForWaiter(authentication, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void updateOrder_success_returnsCreated() {
        UUID orderId = UUID.randomUUID();
        String waiterEmail = "admin@example.com";
        UUID waiterId = UUID.fromString("waiter-uuid-string");
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        OrderResponse updatedOrder = mock(OrderResponse.class);
        when(updatedOrder.getWaiterId()).thenReturn(waiterId);
        when(authentication.getName()).thenReturn(waiterEmail);
        when(orderService.updateOrder(orderId, waiterEmail, request)).thenReturn(updatedOrder);

        ResponseEntity<OrderResponse> response = orderController.updateOrder(orderId, request, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(updatedOrder, response.getBody());
    }

    @Test
    void updateOrder_null_returnsBadRequest() {
        UUID orderId = UUID.randomUUID();
        String waiterEmail = "admin@example.com";
        CreateOrderRequest request = mock(CreateOrderRequest.class);
        when(authentication.getName()).thenReturn(waiterEmail);
        when(orderService.updateOrder(orderId, waiterEmail, request)).thenReturn(null);

        ResponseEntity<OrderResponse> response = orderController.updateOrder(orderId, request, authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void deleteOrder_success_returnsNoContent() {
        UUID orderId = UUID.randomUUID();
        UUID waiterId = UUID.fromString("waiter-uuid-string");
        when(currentUser.getId()).thenReturn(waiterId);

        ResponseEntity<Void> response = orderController.deleteOrder(orderId, currentUser);

        verify(orderService).deleteOrder(orderId, waiterId);
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