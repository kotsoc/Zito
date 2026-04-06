package com.konstantinos.zito.services;

import com.konstantinos.zito.model.*;
import com.konstantinos.zito.model.dto.*;
import com.konstantinos.zito.repositories.OrderRepository;
import com.konstantinos.zito.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {

    private OrderRepository orderRepository;
    private UserRepository userRepository;
    private MenuService menuItemService;
    private SimpMessagingTemplate messagingTemplate;
    private OrderService orderService;

    // Shared test data
    private List<CartItemRequest> cartItemsRequest;
    private OrderItem cartItem1;
    private OrderItem cartItem2;
    private OrderItemModifier itemMod1;
    private OrderItemModifier itemMod2;
    private OrderItemModifier itemMod3;
    private OrderItemModifier itemMod4;
    private static String mod3Id = "mod-3";
    private static String mod4Id = "mod-4";

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        userRepository = mock(UserRepository.class);
        menuItemService = mock(MenuService.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        orderService = new OrderService(orderRepository, userRepository, menuItemService, messagingTemplate);

        // Define Modifiersnull
        var mod1Id = "mod-1";
        var mod2Id = "mod-2";
        itemMod1 = new OrderItemModifier(mod1Id, "extra Cheese", new BigDecimal("3.00"), null, "foodMod");
        itemMod2 = new OrderItemModifier(mod2Id, "extra Cheese", new BigDecimal("2.00"), null, "starterMod");
        itemMod3 = new OrderItemModifier(mod3Id, "Extra Bacon", new BigDecimal("3.00"), null, "foodMod");
        itemMod4 = new OrderItemModifier(mod4Id, "Cheap Bacon", new BigDecimal("2.00"), null, "foodMod");


        // 1. Create Requests using helper
        cartItemsRequest = List.of(
                createCartReq("menu-item-1", 2, List.of(itemMod1), 0),
                createCartReq("menu-item-2", 1, List.of(itemMod2), 5));

        // 2. Create OrderItem Snapshots using helper
        cartItem1 = createOrderItem("menu-item-1", 2, "10");
        cartItem2 = createOrderItem("menu-item-2", 1, "15");

        // 3. Create MenuItems using helper
        MenuItem menuItem1 = createMenuItem("menu-item-1", "10", "13");
        MenuItem menuItem2 = createMenuItem("menu-item-2", "15", "23");

        MenuItem m1 = createMenuItem("m1", "10.00", "13");
        MenuItem m2 = createMenuItem("m2", "20.00", "23");

        when(menuItemService.findAllItemsById(List.of("menu-item-1", "menu-item-2")))
                .thenReturn(List.of(menuItem1, menuItem2));
         when(menuItemService.findAllItemsById(List.of("m1", "m2")))
                .thenReturn(List.of(m1, m2));
    }

    // --- Private Helper Methods ---

    private MenuItem createMenuItem(String id, String netPrice, String taxRate, List<OrderItemModifier> modList) {
        MenuItem m = new MenuItem();
        m.setId(id);
        m.setNetPrice(new BigDecimal(netPrice));
        m.setTaxRate(new BigDecimal(taxRate));
        m.setItemModifiers(modList);
        return m;
    }
    
    private MenuItem createMenuItem(String id, String netPrice, String taxRate) {
        MenuItem m = new MenuItem();
        m.setId(id);
        m.setNetPrice(new BigDecimal(netPrice));
        m.setTaxRate(new BigDecimal(taxRate));
        m.setItemModifiers(List.of());
        return m;
    }

    private CartItemRequest createCartReq(String id, int qty, List<OrderItemModifier> mods, int discount) {
        return new CartItemRequest(id, qty, "Note", mods, discount);
    }

    private OrderItem createOrderItem(String id, int qty, String gross) {
        OrderItem item = new OrderItem();
        item.setMenuItemId(id);
        item.setQuantity(qty);
        item.setGrossTotal(new BigDecimal(gross).setScale(2, RoundingMode.HALF_UP));
        return item;
    }

    @ParameterizedTest
    @MethodSource("provideOrderScenarios")
    void testOrderCalculations(
            List<CartItemRequest> cartItems,
            BigDecimal orderDiscount,
            String expectedNet,
            String expectedGross,
            Map<String, String> expectedTaxes) {
        // GIVEN
        // Setup the Menu Library (Common for all cases)
        MenuItem mi1 = createMenuItem("mi1", "10.00", "13", List.of(itemMod3, itemMod4));
        MenuItem mi2 = createMenuItem("mi2", "20.00", "23");
        MenuItem mi3 = createMenuItem("mi3", "5.00", "0");
        MenuItem mi4 = createMenuItem("mi4", "100.00", "10");
        MenuItem mi5     = createMenuItem("mi5",     "4.333", "24");
        MenuItem mi_free = createMenuItem("mi_free", "0.00",  "0");

        when(menuItemService.findAllItemsById(any())).thenReturn(List.of(mi1, mi2, mi3, mi4, mi5, mi_free));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCartItems(cartItems);
        request.setDiscountPercent(orderDiscount);

        // WHEN
        OrderResponse response = orderService.createGuestOrder(request);

        // THEN
        assertEquals(expectedNet, response.getNetTotal().toString(), "Net Total Mismatch");
        assertEquals(expectedGross, response.getGrossTotal().toString(), "Gross Total Mismatch");

        Map<BigDecimal, BigDecimal> actualTaxes = response.getTaxTotals().stream()
                .collect(Collectors.toMap(TaxEntry::getTaxRate, TaxEntry::getAmount));

        expectedTaxes.forEach((rate, amount) -> {
            BigDecimal rateBD = new BigDecimal(rate);
            assertTrue(actualTaxes.containsKey(rateBD), "Missing Tax Rate: " + rate);
            assertEquals(amount, actualTaxes.get(rateBD).toString(), "Tax Amount Mismatch for rate " + rate);
        });
    }

    // --- Parameter Source for snapshot,tax calculations ---
    private static Stream<Arguments> provideOrderScenarios() {
        // Modifier Definitions
        var extraBacon = new OrderItemModifier(mod3Id, "Extra Bacon", new BigDecimal("3.00"), null, "foodMod");
        var cheapBacon = new OrderItemModifier(mod4Id, "Cheap Bacon", new BigDecimal("2.00"), null, "foodMod");

        return Stream.of(
            // CASE #1: Simple No Discount
            // Args: CartItems, OrderDiscount, ExpNet, ExpGross, ExpTaxes Map
            Arguments.of(
                    List.of(new CartItemRequest("mi1", 1, "", List.of(), 0)),
                    BigDecimal.ZERO, "10.00", "11.30", Map.of("13", "1.30")),

            // CASE #2: Mixed VAT + 10% Discount
            Arguments.of(
                    List.of(
                            new CartItemRequest("mi1", 1, "", List.of(), 0),
                            new CartItemRequest("mi2", 1, "", List.of(), 0)),
                    new BigDecimal("10"), "27.00", "32.31", Map.of("13", "1.17", "23", "4.14")),

            // CASE #3: Modifiers (No Discount) - TDD Target
            Arguments.of(
                    List.of(new CartItemRequest("mi1", 1, "", List.of(extraBacon), 0)),
                    BigDecimal.ZERO, "13.00", "14.69", Map.of("13", "1.69")),

            // CASE #4: Mixed Taxable and Exempt + 20% Discount
            Arguments.of(
                    List.of(
                            new CartItemRequest("mi2", 1, "", List.of(), 0),
                            new CartItemRequest("mi3", 1, "", List.of(), 0)),
                    new BigDecimal("20"), "20.00", "23.68", Map.of("23", "3.68", "0", "0.00")),

            // CASE #5: Modifiers + 50% Discount - Ultimate Test
            Arguments.of(
                    List.of(new CartItemRequest("mi1", 1, "", List.of(cheapBacon), 0)),
                    new BigDecimal("50"), "6.00", "6.78", Map.of("13", "0.78")),
                
            // CASE #6: High Precision Net Price (Common with POS/DB storage)
            // Simulates a DB storing more decimal places than UI
            Arguments.of(
                    List.of(new CartItemRequest("mi5", 2, "", List.of(), 0)), // m5 = 4.333 net, 24% VAT
                    BigDecimal.ZERO, "8.67", "10.75", Map.of("24", "2.08") // VAT extracted from 10.739...
            ),
            // CASE #7: Free Item (Zero Price)
            Arguments.of(
                    List.of(new CartItemRequest("mi_free", 1, "", List.of(), 0)), // m_free = 0.00 net, 0% VAT
                    BigDecimal.ZERO, "0.00", "0.00", Map.of("0", "0.00")
            ),
            // CASE #8: Discount Applied Before VAT Across Rates with Modifiers
            Arguments.of(
                    List.of(
                            new CartItemRequest("mi1", 1, "", List.of(extraBacon), 0), // 10 + 3 = 13 net
                            new CartItemRequest("mi2", 1, "", List.of(), 0)            // 20 net
                    ),
                    new BigDecimal("10"), "29.70", "35.36", Map.of( // 33 net * 0.9 = 29.70
                            "13", "1.52", // 13→0.9=11.7 → VAT=1.521 ≈1.52
                            "23", "4.14"  // 20→0.9=18 → VAT=18*0.23=4.14
                    )
            ),
            // CASE #9: 0% Discount but Mixed Modifiers & Multiple Quantities
            Arguments.of(
                    List.of(
                            new CartItemRequest("mi1", 2, "", List.of(extraBacon), 0), // 2 *10 +(3)=23
                            new CartItemRequest("mi3", 1, "", List.of(), 0)            // 5 0% VAT item
                    ),
                    BigDecimal.ZERO, "28.00", "30.99", Map.of(
                            "13", "2.99",
                            "0", "0.00"
                    )
            )
        );
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

        Optional<OrderResponse> result = orderService.getGuestOrder(order.getId(), cartId, Optional.empty());

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

        Optional<OrderResponse> result = orderService.getGuestOrder(order.getId(), cartId,
                Optional.of("guest@example.com"));

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
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

         // GIVEN: Item 1 (Net 10.00, Tax 13%) | Item 2 (Net 20.00, Tax 23%)
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCartItems(List.of(
                createCartReq("m1", 1, List.of(), 0), // Use helper
                createCartReq("m2", 1, List.of(), 0) // Use helper
        ));
        request.setDiscountPercent(new BigDecimal("10")); // 10% Order Discount

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
    void testUpdateOrder_RecalculatesTotals() {
        // GIVEN: An existing order with 1 item (Net 10.00, Tax 10%)
        UUID waiterId = UUID.randomUUID();
        MenuItem m11 = createMenuItem("m11", "10.00", "10");

        Order existingOrder = new Order();
        existingOrder.setWaiterId(waiterId);
        // Initial totals set to 0 for the test
        existingOrder.setGrossTotal(BigDecimal.ZERO);

        when(orderRepository.findById(existingOrder.getId())).thenReturn(Optional.of(existingOrder));
        when(menuItemService.findAllItemsById(any())).thenReturn(List.of(m11));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN: Update request changes quantity from 1 to 2
        CreateOrderRequest updateRequest = new CreateOrderRequest();
        updateRequest.setOrderPointId("table-1");
        updateRequest.setCartItems(List.of(new CartItemRequest("m11", 2, "", List.of(), 0)));
        updateRequest.setDiscountPercent(BigDecimal.ZERO);

        OrderResponse response = orderService.updateOrder(existingOrder.getId(), waiterId, updateRequest);

        // THEN: Totals should reflect 2 items (20.00 Net + 2.00 Tax)
        assertEquals("20.00", response.getNetTotal().toString());
        assertEquals("22.00", response.getGrossTotal().toString());
        verify(orderRepository).save(any(Order.class));
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

    @Test
    void testCreateOrder_SendsStatusUpdateNotification() {
        // GIVEN
        UUID waiterId = UUID.randomUUID();
        CreateOrderRequest request = new CreateOrderRequest();
        request.setOrderPointId("table-5");
        request.setCartItems(cartItemsRequest);

        when(userRepository.findById(waiterId)).thenReturn(Optional.of(new RestaurantUser()));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN
        OrderResponse response = orderService.createOrder(waiterId, request);

        // THEN: Verify messaging template was called with correct topics
        verify(messagingTemplate, times(3)).convertAndSend(anyString(), any(OrderResponse.class));
        verify(messagingTemplate).convertAndSend("/topic/orders/" + response.getId(), response);
        verify(messagingTemplate).convertAndSend("/topic/tables/table-5", response);
        verify(messagingTemplate).convertAndSend("/topic/orders", response);
    }

    @Test
    void testUpdateOrderStatus_SendsStatusUpdateNotification() {
        // GIVEN
        Order order = new Order();
        UUID orderId = order.getId();
        order.setWaiterId(UUID.randomUUID());
        order.setOrderPointId("table-10");
        order.setStatus(OrderStatus.NEW);
        order.setOrderItems(List.of(cartItem1));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN
        OrderResponse response = orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);

        // THEN: Verify status was updated and WebSocket messages were sent to all 3 topics
        assertEquals(OrderStatus.CONFIRMED, response.getStatus());
        
        // Capture all calls to verify correct topics
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OrderResponse> responseCaptor = ArgumentCaptor.forClass(OrderResponse.class);
        verify(messagingTemplate, times(3)).convertAndSend(topicCaptor.capture(), responseCaptor.capture());
        
        List<String> topics = topicCaptor.getAllValues();
        assertTrue(topics.contains("/topic/orders/" + orderId), 
            "Should send to order-specific topic with correct ID");
        assertTrue(topics.contains("/topic/tables/table-10"), 
            "Should send to table topic");
        assertTrue(topics.contains("/topic/orders"), 
            "Should send to general dashboard topic");
    }

    @Test
    void testUpdateOrderStatus_VerifyCorrectTopicsForMultipleUpdates() {
        // GIVEN
        Order order = new Order();
        UUID orderId = order.getId();
        order.setWaiterId(UUID.randomUUID());
        order.setOrderPointId("table-7");
        order.setStatus(OrderStatus.NEW);
        order.setOrderItems(List.of(cartItem1));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN: First status update
        orderService.updateOrderStatus(orderId, OrderStatus.CONFIRMED);

        // Reset mock state to count new calls for second update
        reset(messagingTemplate);

        // WHEN: Second status update
        orderService.updateOrderStatus(orderId, OrderStatus.READY);

        // THEN: Verify second update also sends to all 3 topics
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<OrderResponse> responseCaptor = ArgumentCaptor.forClass(OrderResponse.class);
        
        verify(messagingTemplate, times(3)).convertAndSend(topicCaptor.capture(), responseCaptor.capture());
        
        List<String> topics = topicCaptor.getAllValues();
        assertTrue(topics.contains("/topic/orders/" + orderId), "Should send to order-specific topic");
        assertTrue(topics.contains("/topic/tables/table-7"), "Should send to table topic");
        assertTrue(topics.contains("/topic/orders"), "Should send to general dashboard topic");
        
        // Verify the status in the response
        OrderResponse sentResponse = responseCaptor.getValue();
        assertEquals(OrderStatus.READY, sentResponse.getStatus());
    }
}
