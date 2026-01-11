package com.konstantinos.zito.services;

import com.konstantinos.zito.model.*;
import com.konstantinos.zito.model.dto.*;
import com.konstantinos.zito.repositories.MenuItemRepository;
import com.konstantinos.zito.repositories.OrderRepository;
import com.konstantinos.zito.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private UserRepository userRepository;
    private MenuService menuItemService;
    private OrderService orderService;
    private List<CartItemRequest> cartItemsRequest;
    private OrderItem cartItem1;
    private OrderItem cartItem2;


    public OrderServiceTest() {
     
    }   

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        userRepository = mock(UserRepository.class);
        menuItemService = mock(MenuService.class);
        orderService = new OrderService(orderRepository, userRepository, menuItemService);
        var itemMod1 = new OrderItemModifier(UUID.randomUUID(), "extra Bacon", BigDecimal.valueOf(3), null, null, null, null);
        var itemMod2 = new OrderItemModifier(UUID.randomUUID(), "extra Cheese", BigDecimal.valueOf(2), null, null, null, null);

        CartItemRequest item1 = new CartItemRequest(
                "menu-item-1", 2, "No onions", List.of(itemMod1), 0
        );
        CartItemRequest item2 = new CartItemRequest(
                "menu-item-2", 1, "No tomatoes", List.of(itemMod2), 5
        );
        cartItemsRequest = List.of(item1, item2);

        cartItem1 = new OrderItem();
        cartItem1.setMenuItemId("menu-item-1");
        cartItem1.setQuantity(2);
        cartItem1.setGrossTotal(BigDecimal.valueOf(10));

        cartItem2 = new OrderItem();
        cartItem2.setMenuItemId("menu-item-2");
        cartItem2.setQuantity(1);
        cartItem2.setGrossTotal(BigDecimal.valueOf(15));

        MenuItem menuItem1 = new MenuItem();
        menuItem1.setId("menu-item-1");
        menuItem1.setNetPrice(BigDecimal.valueOf(10));
        menuItem1.setTaxRate(BigDecimal.valueOf(13));

        MenuItem menuItem2 = new MenuItem();
        menuItem2.setId("menu-item-2");
        menuItem2.setNetPrice(BigDecimal.valueOf(15));
        menuItem2.setTaxRate(BigDecimal.valueOf(23));
        var menulist = List.of(menuItem1, menuItem2);

        when(menuItemService.findAllItemsById(List.of("menu-item-1", "menu-item-2")))
                .thenReturn(menulist);
    //     when(menuItemService.findById("menu-item-1"))
    //             .thenReturn(Optional.of(menuItem1));
    //     when(menuItemService.findById("menu-item-2"))
    //             .thenReturn(Optional.of(menuItem2));
    }

    @Test
    void testCreateGuestOrder_Success() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setOrderPointId("table-1");
        UUID cartId = UUID.randomUUID();
        request.setCartId(cartId);
        request.setCartItems(cartItemsRequest);
        request.setGuestEmail("guest@example.com");

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse response = orderService.createGuestOrder(request);

        verify(orderRepository).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();

        assertEquals("table-1", savedOrder.getOrderPointId());
        assertEquals(cartId, savedOrder.getCartId());
        assertEquals("guest@example.com", savedOrder.getGuestEmail());
        assertEquals(OrderStatus.NEW, savedOrder.getStatus());
        assertFalse(savedOrder.isPaid());
        assertEquals(2, savedOrder.getOrderItems().size());

        assertEquals(savedOrder.getId(), response.getId());
    }

    @Test
    void testGetGuestOrder_FoundByCartId() {
        UUID cartId = UUID.randomUUID();
        Order order = new Order();
        order.setCartId(cartId);
        order.setGuestEmail("guest@example.com");
        order.setOrderItems(List.of(cartItem1));

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        Optional<OrderResponse> result =
                orderService.getGuestOrder(order.getId(), cartId, Optional.empty());

        assertTrue(result.isPresent());
        assertEquals(order.getId(), result.get().getId());
    }

    @Test
    void testGetGuestOrder_FoundByGuestEmail() {
        UUID cartId = UUID.randomUUID();
        Order order = new Order();
        order.setCartId(cartId);
        order.setGuestEmail("guest@example.com");
        order.setOrderItems(List.of(cartItem1));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        Optional<OrderResponse> result = orderService.getGuestOrder(order.getId(), cartId, Optional.of("guest@example.com"));

        assertTrue(result.isPresent());
        assertEquals(order.getId(), result.get().getId());
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
        UUID waiterId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest();
        request.setOrderPointId("table-2");
        request.setCartItems(cartItemsRequest);

        when(userRepository.findById(waiterId))
                .thenReturn(Optional.of(new RestaurantUser()));

        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderResponse response = orderService.createOrder(waiterId, request);

        assertNotNull(response.getId());
    }

    @Test
    void testCreateOrder_WaiterNotFound() {
        UUID waiterId = UUID.randomUUID();
        when(userRepository.findById(waiterId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.createOrder(waiterId, new CreateOrderRequest()));
        assertEquals("Waiter not found", ex.getMessage());
    }

    @Test
    void testGetOrders_ReturnsOrders() {
        UUID waiterId = UUID.randomUUID();
        Order order = new Order();
        order.setWaiterId(waiterId);
        order.setOrderItems(List.of(cartItem1));

        when(orderRepository.findByWaiterId(waiterId)).thenReturn(List.of(order));

        List<OrderResponse> responses = orderService.getUserOrder(waiterId);

        assertEquals(1, responses.size());
        assertEquals(waiterId, responses.get(0).getWaiterId());
    }

    @Test
    void testGetOrdersByTable_ReturnsOrders() {
        String tableNumber = "table-3";
        Order order = new Order();
        order.setOrderPointId("table-3");
        order.setOrderItems(List.of(cartItem1, cartItem2));

        when(orderRepository.findByOrderPointId(tableNumber)).thenReturn(List.of(order));

        List<OrderResponse> responses = orderService.getOrdersByTable(tableNumber);

        assertEquals(1, responses.size());
        assertEquals(order.getOrderPointId(), responses.get(0).getOrderPointId());
    }

    @Test
    void testOrderTotals_MixedVatAndOrderDiscount() {
        // GIVEN: Item 1 (Net 10.00, Tax 13%) | Item 2 (Net 20.00, Tax 23%)
        MenuItem m1 = new MenuItem(); m1.setId("m1"); m1.setNetPrice(new BigDecimal("10.00")); m1.setTaxRate(new BigDecimal("13"));
        MenuItem m2 = new MenuItem(); m2.setId("m2"); m2.setNetPrice(new BigDecimal("20.00")); m2.setTaxRate(new BigDecimal("23"));

        when(menuItemService.findAllItemsById(any())).thenReturn(List.of(m1, m2));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCartItems(List.of(
            new CartItemRequest("m1", 1, "", List.of(), 0),
            new CartItemRequest("m2", 1, "", List.of(), 0)
        ));
        request.setDiscount(new BigDecimal("10")); // 10% Order Discount

        // WHEN
        OrderResponse response = orderService.createGuestOrder(request);
        
        // THEN: Math Verification (Net First Logic)
        // Total Net before discount = 30.00
        // Order Discount (10%) = 3.00
        // Item 1 New Net: 10.00 - (1/3 of 3.00) = 9.00 -> Tax: 9.00 * 0.13 = 1.17
        // Item 2 New Net: 20.00 - (2/3 of 3.00) = 18.00 -> Tax: 18.00 * 0.23 = 4.14
        
        assertEquals("27.00", response.getNetTotal().toString());
        
        // Check individual tax entries
        Map<BigDecimal, BigDecimal> taxes = response.getTaxTotals().stream()
                .collect(Collectors.toMap(TaxEntry::getTaxRate, TaxEntry::getAmount));
                
        assertEquals("1.17", taxes.get(new BigDecimal("13")).toString());
        assertEquals("4.14", taxes.get(new BigDecimal("23")).toString());
        
        // Total Gross: 27.00 + 1.17 + 4.14 = 32.31
        assertEquals("32.31", response.getGrossTotal().toString());
    }

    @Test
    void testUpdateOrder_Success() {
        UUID waiterId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest();
        request.setOrderPointId("table-4");
        request.setCartItems(cartItemsRequest);

        Order existingOrder = new Order();
        existingOrder.setWaiterId(waiterId);
        existingOrder.setOrderItems(new ArrayList<>()); // empty initially

        // Mock MenuItemRepository for price snapshot
        for (CartItemRequest item : cartItemsRequest) {
            MenuItem menuItem = new MenuItem();
            menuItem.setId(item.getMenuItemId());
            menuItem.setGrossPrice(BigDecimal.valueOf(10)); // just a fixed test price
        }

        when(orderRepository.findById(existingOrder.getId())).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(existingOrder);

        OrderResponse response = orderService.updateOrder(existingOrder.getId(), waiterId, request);

        verify(orderRepository).save(existingOrder);
        assertEquals(existingOrder.getId(), response.getId());
        assertEquals(waiterId, response.getWaiterId());

        assertEquals("table-4", response.getOrderPointId());
        assertEquals(cartItemsRequest.size(), response.getOrderItems().size());
    }

    @Test
    void testUpdateOrder_OrderNotFound() {
        UUID orderId = UUID.randomUUID();
        UUID waiterId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.updateOrder(orderId, waiterId, request));
        assertEquals("Order not found", ex.getMessage());
    }

    @Test
    void testDeleteOrder_Success() {
        UUID orderId = UUID.randomUUID();
        UUID waiterId = UUID.randomUUID();
        Order order = new Order();
        order.setWaiterId(waiterId);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.deleteOrder(orderId, waiterId);

        verify(orderRepository).delete(order);
    }

    @Test
    void testDeleteOrder_WaiterMismatch() {
        UUID orderId = UUID.randomUUID();
        UUID waiterId = UUID.randomUUID();
        Order order = new Order();
        order.setWaiterId(UUID.randomUUID()); // mismatch

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.deleteOrder(orderId, waiterId));
        assertEquals("Waiter mismatch", ex.getMessage());
    }

    @Test
    void testDeleteOrder_OrderNotFound() {
        UUID orderId = UUID.randomUUID();
        UUID waiterId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> orderService.deleteOrder(orderId, waiterId));
        assertEquals("Order not found", ex.getMessage());
    }
}
