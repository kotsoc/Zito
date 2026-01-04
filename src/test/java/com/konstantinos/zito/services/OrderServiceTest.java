package com.konstantinos.zito.services;

import com.konstantinos.zito.model.*;
import com.konstantinos.zito.model.dto.*;
import com.konstantinos.zito.repositories.OrderRepository;
import com.konstantinos.zito.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private UserRepository userRepository;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        userRepository = mock(UserRepository.class);
        orderService = new OrderService(orderRepository, userRepository);
    }

    @Test
    void testCreateGuestOrder_Success() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setOrderPointId("table-1");
        request.setCartId(UUID.randomUUID());
        request.setCartItems(List.of(createCartItemRequest()));
        request.setGuestEmail(Optional.of("guest@example.com"));

        Order savedOrder = new Order();
        savedOrder.setOrderPointId(request.getOrderPointId());
        savedOrder.setCartId(request.getCartId());
        savedOrder.setGuestEmail("guest@example.com");
        savedOrder.setCartItems(List.of(new CartItem()));
        savedOrder.setPaid(false);
        savedOrder.setStatus(OrderStatus.NEW);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.createGuestOrder(request);

        assertNotNull(response);
        assertEquals(savedOrder.getId(), response.getId());
        assertEquals(savedOrder.getOrderPointId(), response.getOrderPointId());
        assertFalse(response.isPaid());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testGetGuestOrder_FoundByOrderIdAndCartId() {
        UUID orderId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        Order order = new Order();
        order.setCartId(cartId);
        order.setGuestEmail("guest@example.com");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Optional<OrderResponse> result = orderService.getGuestOrder(orderId, cartId, Optional.empty());

        assertTrue(result.isPresent());
        assertEquals(orderId, result.get().getId());
    }

    @Test
    void testGetGuestOrder_FoundByGuestEmail() {
        UUID orderId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        Order order = new Order();
        order.setCartId(UUID.randomUUID());
        order.setGuestEmail("guest@example.com");
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Optional<OrderResponse> result = orderService.getGuestOrder(orderId, cartId, Optional.of("guest@example.com"));

        assertTrue(result.isPresent());
        assertEquals(orderId, result.get().getId());
    }

    @Test
    void testGetGuestOrder_NotFound() {
        UUID orderId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        Optional<OrderResponse> result = orderService.getGuestOrder(orderId, cartId, Optional.empty());

        assertFalse(result.isPresent());
    }

    @Test
    void testCreateOrder_Success() {
        String waiterEmail = "waiter@example.com";
        UUID waiterIdString = UUID.fromString("waiter-uuid-string");
        CreateOrderRequest request = new CreateOrderRequest();
        request.setOrderPointId("table-2");
        request.setCartItems(List.of(createCartItemRequest()));

        when(userRepository.findByEmail(waiterEmail)).thenReturn(Optional.of(new RestaurantUser()));
        Order savedOrder = new Order();

        savedOrder.setWaiterId(waiterIdString);
        savedOrder.setOrderPointId(request.getOrderPointId());
        savedOrder.setCartItems(List.of(new CartItem()));
        savedOrder.setPaid(false);
        savedOrder.setStatus(OrderStatus.NEW);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.createOrder(waiterIdString, request);

        assertNotNull(response);
        assertEquals(savedOrder.getId(), response.getId());
        assertEquals(savedOrder.getWaiterId(), response.getWaiterId());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void testCreateOrder_WaiterNotFound() {
        UUID waiterIdString = UUID.fromString("waiter-uuid-string");
        when(userRepository.findById(waiterIdString)).thenReturn(Optional.empty());
        var request = new CreateOrderRequest();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.createOrder(waiterIdString, request));
        assertEquals("Waiter not found", ex.getMessage());
    }

    @Test
    void testGetUserOrder_ReturnsOrders() {
        UUID waiterId = UUID.fromString("waiter-uuid-string");
        Order order = new Order();
        order.setWaiterId(waiterId);
        order.setCartItems(List.of(new CartItem()));
        when(orderRepository.findByWaiterId(waiterId)).thenReturn(List.of(order));

        List<OrderResponse> responses = orderService.getUserOrder(waiterId);

        assertEquals(1, responses.size());
        assertEquals(order.getId(), responses.get(0).getId());
    }

    @Test
    void testGetOrdersByTable_ReturnsOrders() {
        String orderPointId = "table-3";
        Order order = new Order();
        order.setOrderPointId(orderPointId);
        order.setCartItems(List.of(new CartItem()));
        when(orderRepository.findByOrderPointId(orderPointId)).thenReturn(List.of(order));

        List<OrderResponse> responses = orderService.getOrdersByTable(orderPointId);

        assertEquals(1, responses.size());
        assertEquals(order.getOrderPointId(), responses.get(0).getOrderPointId());
    }

    @Test
    void testUpdateOrder_Success() {
        UUID orderId = UUID.randomUUID();
        String waiterEmail = "waiter@example.com";
        UUID waiterId = UUID.fromString("waiter-uuid-string");
        CreateOrderRequest request = new CreateOrderRequest();
        request.setOrderPointId("table-4");
        request.setCartItems(List.of(createCartItemRequest()));

        Order existingOrder = new Order();
        existingOrder.setWaiterId(waiterId);
        existingOrder.setCartItems(new ArrayList<>());

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(existingOrder);

        OrderResponse response = orderService.updateOrder(orderId, waiterEmail, request);

        assertNotNull(response);
        assertEquals(orderId, response.getId());
        assertEquals("table-4", response.getOrderPointId());
        verify(orderRepository).save(existingOrder);
    }

    @Test
    void testUpdateOrder_OrderNotFound() {
        UUID orderId = UUID.randomUUID();
        String waiterEmail = "waiter@example.com";
        CreateOrderRequest request = new CreateOrderRequest();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.updateOrder(orderId, waiterEmail, request));
        assertEquals("Order not found", ex.getMessage());
    }

    @Test
    void testDeleteOrder_Success() {
        UUID orderId = UUID.randomUUID();
        UUID waiterId = UUID.fromString("waiter-uuid-string");
        Order order = new Order();
        order.setWaiterId(UUID.fromString("waiter-uuid-string"));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.deleteOrder(orderId, waiterId);

        verify(orderRepository).delete(order);
    }

    @Test
    void testDeleteOrder_WaiterMismatch() {
        UUID orderId = UUID.randomUUID();
        UUID waiterId = UUID.fromString("waiter-uuid-string");
        Order order = new Order();

        order.setWaiterId(waiterId);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.deleteOrder(orderId, UUID.randomUUID()));
        assertEquals("Waiter mismatch", ex.getMessage());
    }

    @Test
    void testDeleteOrder_OrderNotFound() {
        UUID orderId = UUID.randomUUID();
        UUID waiterId = UUID.fromString("waiter-uuid-string");
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> orderService.deleteOrder(orderId, waiterId));
        assertEquals("Order not found", ex.getMessage());
    }

    // --- Helper methods ---

    private CartItemRequest createCartItemRequest() {
        CartItemRequest req = new CartItemRequest();
        req.setMenuItemId("menu-item-1");
        req.setQuantity(2);
        req.setNote("No onions");
        req.setModifiers(List.of("extra cheese"));
        return req;
    }
}