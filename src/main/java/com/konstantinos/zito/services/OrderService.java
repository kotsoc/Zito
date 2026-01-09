package com.konstantinos.zito.services;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;

import com.konstantinos.zito.repositories.MenuItemRepository;
import com.konstantinos.zito.repositories.OrderRepository;
import com.konstantinos.zito.repositories.UserRepository;
import com.konstantinos.zito.util.OrderTotals;
import com.konstantinos.zito.model.Order;
import com.konstantinos.zito.model.OrderStatus;
import com.konstantinos.zito.model.OrderItem;
import com.konstantinos.zito.model.dto.OrderResponse;
import com.konstantinos.zito.model.dto.CreateOrderRequest;
import com.konstantinos.zito.model.dto.CartItemRequest;
import com.konstantinos.zito.model.dto.CartItemResponse;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    private final UserRepository userRepository;

    private final MenuItemRepository menuItemRepository;

    public OrderService(OrderRepository orderRepository,
                        UserRepository userRepository,
                        MenuItemRepository menuItemRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.menuItemRepository = menuItemRepository;
    }

     public OrderResponse createGuestOrder(CreateOrderRequest request) {

        Order order = new Order();
        order.setOrderPointId(request.getOrderPointId());
        order.setCartId(request.getCartId());
        order.setCreated_At(new Date());
        order.setLatest_update(new Date());
        order.setOrderItems(calculateCartSnapshot(request));
        order.setPaid(false);
        order.setStatus(OrderStatus.NEW);
        var orderTotals = calculateOrderTotals(order.getOrderItems());
        order.setSubTotal(orderTotals.getSubtotal());
        order.setTaxTotals(orderTotals.getTaxTotalsByRate());
        order.setGrandTotal(orderTotals.getGrandTotal());


        //email is optional
        if (request.getGuestEmail() != null){order.setGuestEmail(request.getGuestEmail());}

        return mapToResponse(orderRepository.save(order));
    }

     public Optional<OrderResponse> getGuestOrder(UUID orderId, UUID cartId, Optional<String> guestEmail) {
        // Try to find by orderId and cartId first
       return orderRepository.findById(orderId).filter(order -> order.getCartId().equals(cartId) 
       || guestEmail.map(email -> email.equals(order.getGuestEmail())).orElse(false))
       .map(this::mapToResponse);
    }

    // --- Create a new order --- by authenticated user
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        var waiterOpt = userRepository.findById(userId);
        if (waiterOpt.isEmpty()) {
            throw new RuntimeException("Waiter not found");
        }

        Order order = new Order();
        order.setWaiterId(userId);
        order.setOrderPointId(request.getOrderPointId());
        order.setCreated_At(new Date());
        order.setLatest_update(new Date());
        order.setOrderItems(calculateCartSnapshot(request));

        order.setPaid(false); // default
        order.setStatus(OrderStatus.NEW); // default or NEW
        var orderTotals = calculateOrderTotals(order.getOrderItems());
        order.setSubTotal(orderTotals.getSubtotal());
        order.setTaxTotals(orderTotals.getTaxTotalsByRate());
        order.setGrandTotal(orderTotals.getGrandTotal());

        return mapToResponse(orderRepository.save(order));
    }

    public List<OrderResponse> getUserOrder(UUID viewerId) {
            return orderRepository.findByWaiterId(viewerId).stream()
                    .map(this::mapToResponse).toList();
    }

    public List<OrderResponse> getOrdersByTable(String orderPointId) {
        return orderRepository.findByOrderPointId(orderPointId).stream()
                .map(this::mapToResponse).toList();
    }


    // --- Update order ---
    public OrderResponse updateOrder(UUID orderId, UUID waiterId, CreateOrderRequest request) {
        Order existingOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!existingOrder.getWaiterId().equals(waiterId)) {
            throw new RuntimeException("Waiter mismatch");
        }

        existingOrder.setOrderItems(calculateCartSnapshot(request));
        existingOrder.setLatest_update(new Date());
        existingOrder.setOrderPointId(request.getOrderPointId());

        return mapToResponse(orderRepository.save(existingOrder));
    }

    // --- Delete order ---
    public void deleteOrder(UUID orderId, UUID waiterId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getWaiterId().equals(waiterId)) {
            //TODO: custom exception
            throw new RuntimeException("Waiter mismatch");
        }
        orderRepository.delete(order);
    }

    // // --- Mapping helpers ---
    // private Optional<CartItem>mapToCartItem(CartItemRequest request) {
    //     var menuItem = menuItemRepository.findById(request.getMenuItemId());
    //     if (menuItem.isPresent()) {
    //         CartItem ci = new CartItem();
    //                 ci.setMenuItemId(menuItem.get().getId());
    //                 ci.setName(menuItem.get().getName());
    //                 ci.setUnitPrice(menuItem.get().getPrice());
    //                 ci.setQuantity(request.getQuantity());
    //                 ci.setNote(request.getNote());
    //                 ci.setModifiers(request.getModifiers());
    //                 ci.setTotalPrice(menuItem.get().getPrice()
    //                     .multiply(BigDecimal.valueOf(ci.getQuantity()).setScale(2, RoundingMode.HALF_EVEN)
    //                     .subtract(ci.getDiscount() != null ? ci.getDiscount() : BigDecimal.ZERO)));
    //         return Optional.of(ci);
    //     }
    //     return Optional.empty();
    // }

    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setWaiterId(order.getWaiterId());
        response.setOrderPointId(order.getOrderPointId());
        response.setCreatedAt(order.getCreated_At());
        response.setLatestUpdate(order.getLatest_update());
        response.setPaid(order.isPaid());
        response.setCartItems(order.getOrderItems().stream().map(ci -> {
            var ciResp = new CartItemResponse();
            ciResp.setMenuItemId(ci.getMenuItemId());
            ciResp.setName(ci.getName());
            ciResp.setUnitPrice(ci.getUnitPrice());
            ciResp.setQuantity(ci.getQuantity());
            ciResp.setDiscount(ci.getDiscount());
            ciResp.setNote(ci.getNote());
            ciResp.setModifiers(ci.getItemModifiers());
            ciResp.setTax(ci.getTaxRate());
            ciResp.setSubtotal(ci.getSubTotal());
            return ciResp;
        }).toList());
        response.setSubtotal(order.getSubTotal());
        response.setTaxTotals(order.getTaxTotals());
        response.setGrandTotal(order.getGrandTotal());
        return response;
    }

    private List<OrderItem> calculateCartSnapshot(CreateOrderRequest orderRequest) {
      List<OrderItem> cartSnapshot = new ArrayList<>();
        orderRequest.getCartItems().forEach(ciReq -> {  
            
            var menuItem = menuItemRepository.findById(ciReq.getMenuItemId())
                .orElseThrow(() -> new RuntimeException("Menu item not found"));

            OrderItem ci = new OrderItem();
            ci.setMenuItemId(menuItem.getId());
            ci.setName(menuItem.getName());
            ci.setUnitPrice(menuItem.getPrice());
            ci.setQuantity(ciReq.getQuantity());
            ci.setTaxRate(menuItem.getTaxRate());
            ci.setNote(ciReq.getNote());
            ci.setItemModifiers(ciReq.getModifiers());
            ci.setSubTotal(menuItem.getPrice()
                .multiply(BigDecimal.valueOf(ci.getQuantity()).setScale(2, RoundingMode.HALF_EVEN)
                .subtract(ci.getDiscount() != null ? ci.getDiscount() : BigDecimal.ZERO)));
            cartSnapshot.add(ci);
        });
        return cartSnapshot;
    }

    public OrderTotals calculateOrderTotals(List<OrderItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;
        Map<BigDecimal, BigDecimal> taxTotals = new HashMap<>();

        for (OrderItem item : items) {
            BigDecimal itemSubtotal = item.getSubTotal();
            subtotal = subtotal.add(itemSubtotal);

            if (item.getTaxRate() != null) {
                BigDecimal taxAmount = itemSubtotal.multiply(item.getTaxRate())
                                                .setScale(2, RoundingMode.HALF_UP);
                taxTotals.merge(item.getTaxRate(), taxAmount, BigDecimal::add);
            }
        }

        taxTotals.values().stream().map(a -> a.setScale(2, RoundingMode.HALF_UP));
        grandTotal = subtotal.add(taxTotals.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        grandTotal.setScale(2, RoundingMode.HALF_UP);

        return new OrderTotals(grandTotal, subtotal, taxTotals);
    }
}