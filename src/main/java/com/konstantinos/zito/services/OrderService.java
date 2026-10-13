package com.konstantinos.zito.services;

import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators.Sum;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;

import com.konstantinos.zito.repositories.MenuItemRepository;
import com.konstantinos.zito.repositories.OrderRepository;
import com.konstantinos.zito.repositories.UserRepository;
import com.konstantinos.zito.util.OrderTotals;
import com.konstantinos.zito.model.MenuItem;
import com.konstantinos.zito.model.Order;
import com.konstantinos.zito.model.OrderStatus;
import com.konstantinos.zito.model.TaxEntry;
import com.konstantinos.zito.model.OrderItem;
import com.konstantinos.zito.model.OrderItemModifier;
import com.konstantinos.zito.model.dto.OrderResponse;
import com.konstantinos.zito.model.dto.CreateOrderRequest;
import com.konstantinos.zito.model.dto.CartItemRequest;
import com.konstantinos.zito.model.dto.OrderItemResponse;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    private final UserRepository userRepository;

    private final MenuService menuService;
    private final SimpMessagingTemplate messagingTemplate;

    public OrderService(OrderRepository orderRepository,
            UserRepository userRepository,
            MenuService menuService,
            SimpMessagingTemplate messagingTemplate) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.menuService = menuService;
        this.messagingTemplate = messagingTemplate;
    }

    public OrderResponse createGuestOrder(CreateOrderRequest request) {
        validateDiscounts(request, false);

        Order order = new Order();
        order.setOrderPointId(request.getOrderPointId());
        order.setCartId(request.getCartId());
        order.setCreated_At(new Date());
        order.setLatest_update(new Date());
        order.setOrderDiscount(request.getDiscountPercent());
        order.setPaid(false);
        order.setStatus(OrderStatus.NEW);

        // email is optional
        if (request.getGuestEmail() != null) {
            order.setGuestEmail(request.getGuestEmail());
        }

        // Add items and create snapshots
        order.setOrderItems(calculateCartSnapshot(request.getCartItems()));

        // calculate order totals
        var orderTotals = calculateOrderTotals(order.getOrderItems(), request.getDiscountPercent());
        order.setNetTotal(orderTotals.getNetTotal());
        order.setTaxTotals(orderTotals.getTaxTotalsByRate());
        order.setGrossTotal(orderTotals.getGrossTotal());

        return mapToResponse(orderRepository.save(order));
    }

    public Optional<OrderResponse> getGuestOrder(UUID orderId, UUID cartId, Optional<String> guestEmail) {
        // Try to find by orderId and cartId first
        return orderRepository.findById(orderId).filter(order -> order.getCartId().equals(cartId)
                || guestEmail.map(email -> email.equals(order.getGuestEmail())).orElse(false))
                .map(this::mapToResponse);
    }

    
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    // --- Create a new order --- by authenticated user
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        var waiterOpt = userRepository.findById(userId);
        if (waiterOpt.isEmpty()) {
            throw new RuntimeException("Waiter not found");
        }
        validateDiscounts(request, true);

        Order order = new Order();
        order.setWaiterId(userId);
        order.setOrderPointId(request.getOrderPointId());
        order.setCreated_At(new Date());
        order.setLatest_update(new Date());
        order.setOrderDiscount(request.getDiscountPercent());
        order.setPaid(false);
        order.setStatus(OrderStatus.NEW);

        // Add items and create snapshots
        order.setOrderItems(calculateCartSnapshot(request.getCartItems()));

        // calculate order totals
        var orderTotals = calculateOrderTotals(order.getOrderItems(), request.getDiscountPercent());
        order.setNetTotal(orderTotals.getNetTotal());
        order.setTaxTotals(orderTotals.getTaxTotalsByRate());
        order.setGrossTotal(orderTotals.getGrossTotal());

        Order savedOrder = orderRepository.save(order);                                                                                                                                                                                             
        notifyStatusUpdate(savedOrder);                                                                                                                                                                                                             
        return mapToResponse(savedOrder);   
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

        if (existingOrder.getStatus().compareTo(OrderStatus.CONFIRMED) > 0) {
            throw new RuntimeException("Cannot change order, it is already being prepared!");

        }
        // TODO:: ADD TO CHANGED ORDERS REPOSITORY
        validateDiscounts(request, true);
        existingOrder.setOrderItems(calculateCartSnapshot(request.getCartItems()));
        existingOrder.setLatest_update(new Date());
        existingOrder.setOrderPointId(request.getOrderPointId());

        var orderTotals = calculateOrderTotals(existingOrder.getOrderItems(), request.getDiscountPercent());
        existingOrder.setGrossTotal(orderTotals.getGrossTotal());
        existingOrder.setTaxTotals(orderTotals.getTaxTotalsByRate());
        existingOrder.setNetTotal(orderTotals.getNetTotal());
        existingOrder.setDiscountTotal(orderTotals.getDiscountTotal());

        return mapToResponse(orderRepository.save(existingOrder));
    }

    // --- Delete order ---
    public void deleteOrder(UUID orderId, UUID waiterId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (!order.getWaiterId().equals(waiterId)) {
            // TODO: custom exception
            throw new RuntimeException("Waiter mismatch");
        }
        orderRepository.delete(order);
    }

    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCartId(order.getCartId());
        response.setWaiterId(order.getWaiterId());
        response.setOrderPointId(order.getOrderPointId());
        response.setCreatedAt(order.getCreated_At());
        response.setLatestUpdate(order.getLatest_update());
        response.setPaid(order.isPaid());
        response.setStatus(order.getStatus());
        response.setOrderItems(order.getOrderItems().stream().map(ci -> {
            var ciResp = new OrderItemResponse();
            ciResp.setMenuItemId(ci.getMenuItemId());
            ciResp.setName(ci.getName());
            ciResp.setUnitPrice(ci.getUnitPriceGross());
            ciResp.setQuantity(ci.getQuantity());
            ciResp.setDiscount(ci.getItemDiscount());
            ciResp.setNote(ci.getNote());
            ciResp.setModifiers(ci.getItemModifiers());
            ciResp.setTax(ci.getUnitTax());
            ciResp.setLineTotal(ci.getGrossTotal()); // Gross for EU
            return ciResp;
        }).toList());
        response.setNetTotal(order.getNetTotal());
        response.setGrossTotal(order.getGrossTotal());
        response.setTaxTotals(order.getTaxTotals());
        return response;
    }

    private void validateDiscounts(CreateOrderRequest request, boolean staffOrder) {
        BigDecimal orderDiscount = request.getDiscountPercent();
        if (orderDiscount != null) {
            validateDiscountRate(orderDiscount);
            if (!staffOrder && orderDiscount.signum() != 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guests cannot apply discounts");
            }
        }
        if (request.getCartItems() != null) {
            for (CartItemRequest item : request.getCartItems()) {
                validateDiscountRate(BigDecimal.valueOf(item.getItemDiscount()));
                if (!staffOrder && item.getItemDiscount() != 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Guests cannot apply discounts");
                }
            }
        }
    }

    private void validateDiscountRate(BigDecimal discount) {
        if (discount.signum() < 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Discount must be between 0 and 100 percent");
        }
    }

    // Item level discount - For each item, calculate the gross price after any item
    // discount.
    // Discounted Net Price = Net Price × (1 – Item Discount %)
    // VAT = Discounted Net Price × VAT Rate
    // Gross Price = Discounted Net Price + VAT
    private List<OrderItem> calculateCartSnapshot(List<CartItemRequest> cartItems) {
        var menuItemList = menuService
                .findAllItemsById(cartItems.stream().map(CartItemRequest::getMenuItemId).toList());
        Map<String, MenuItem> menuItemMap = menuItemList.stream().collect(
                Collectors.toMap(MenuItem::getId, item -> item));

        return cartItems.stream().map(ciReq -> {
            if (menuItemMap.size() < cartItems.size()) {
                throw new RuntimeException("Found fewer Menu Items than request, wrong MenuItem id passed");
            }
            var menuItem = menuItemMap.get(ciReq.getMenuItemId());
            Map<String, BigDecimal> identifierPriceMap = menuItem.getItemModifiers() != null
                    ? menuItem.getItemModifiers().stream().collect(
                            Collectors.toMap(
                                    OrderItemModifier::getId,
                                    item -> item.getNetPrice() != null ? item.getNetPrice() : BigDecimal.ZERO))
                    : Map.of();
            // Set base information
            OrderItem item = new OrderItem();
            item.setMenuItemId(menuItem.getId());
            item.setName(menuItem.getName());

            // From cartRequest
            item.setQuantity(ciReq.getQuantity());
            item.setNote(ciReq.getNote());
            item.setItemModifiers(
            Optional.ofNullable(ciReq.getModifiers()) // Wrap the potentially null list
                .orElse(List.of())                    // Default to empty list if null
                .stream()
                .map(m -> {
                    m.setNetPrice(identifierPriceMap.getOrDefault(m.getId(), BigDecimal.ZERO));
                    return m;
                }).toList()
            );
            item.setItemDiscount(
                    ciReq.getItemDiscount() != 0 ? BigDecimal.valueOf(ciReq.getItemDiscount()) : BigDecimal.ZERO);

            // 1. Base Prices: Start with the correct unit Gross Price to prevent multi-unit cent miscalculations
            BigDecimal unitGross = menuItem.getGrossPrice() != null 
                    ? menuItem.getGrossPrice() 
                    : calculateEUGross(menuItem.getNetPrice(), menuItem.getTaxRate());
                    
            BigDecimal lineGrossTotal = unitGross.multiply(BigDecimal.valueOf(item.getQuantity()));

            // 2. Add Gross values of Modifiers
            if (item.getItemModifiers() != null) {
                BigDecimal modifiersGrossSum = item.getItemModifiers().stream()
                        .map(m -> calculateEUGross(m.getNetPrice(), menuItem.getTaxRate()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                lineGrossTotal = lineGrossTotal.add(modifiersGrossSum);
            }

            // 3. Apply Item Discount against the Line Gross
            BigDecimal itemDiscountAmount = BigDecimal.ZERO;
            if (item.getItemDiscount().compareTo(BigDecimal.ZERO) > 0) {
                 itemDiscountAmount = lineGrossTotal.multiply(item.getItemDiscount())
                         .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP); 
            }
            
            BigDecimal finalLineGross = lineGrossTotal.subtract(itemDiscountAmount).setScale(2, RoundingMode.HALF_UP);
            item.setDiscountTotal(itemDiscountAmount.setScale(2, RoundingMode.HALF_UP));

            // 4. Reverse calculate Net and Tax from Final Gross to guarantee Exact Sums
            BigDecimal taxRateMultiplier = BigDecimal.ONE.add(menuItem.getTaxRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
            BigDecimal finalLineNet = finalLineGross.divide(taxRateMultiplier, 2, RoundingMode.HALF_UP);
            BigDecimal finalLineTax = finalLineGross.subtract(finalLineNet); // Forces Tax = Gross - Net
            
            TaxEntry taxEntry = new TaxEntry(menuItem.getTaxRate(), finalLineTax);
            item.setTaxEntry(taxEntry);
            item.setNetTotal(finalLineNet);
            item.setGrossTotal(finalLineGross);

            return item;
        }).toList();
    }

    // Order-Level Calculation - Sum all item gross prices, then apply the order
    // discount.
    // Subtotal Gross = Sum of all item gross prices
    // Order Discount = Subtotal Gross × Order Discount %
    // Final Gross Total = Subtotal Gross – Order Discount
    public OrderTotals calculateOrderTotals(List<OrderItem> items, BigDecimal discountRate) {
        BigDecimal orderNetAfterDiscount = BigDecimal.ZERO;
        BigDecimal orderGrossTotal = BigDecimal.ZERO;
        Map<BigDecimal, BigDecimal> taxTotals = new HashMap<>();

        // 1. Calculate Initial Total Gross from all item gross amounts
        BigDecimal totalGross = items.stream()
                .map(OrderItem::getGrossTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal orderDiscount = BigDecimal.ZERO;
        
        // 2. Apply Order-Level Discount Pro-rata against Gross items
        if (discountRate != null && discountRate.compareTo(BigDecimal.ZERO) > 0) {
            orderDiscount = totalGross.multiply(discountRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            BigDecimal discountConsumed = BigDecimal.ZERO;
            for (int i = 0; i < items.size(); i++) {
                var item = items.get(i);
                BigDecimal discountForItem;

                if (i == items.size() - 1) {
                    // Last item catches the rounding remainder
                    discountForItem = orderDiscount.subtract(discountConsumed);
                } else {
                    // Distribute proportionately by Gross
                    BigDecimal proportion = totalGross.compareTo(BigDecimal.ZERO) > 0 ? item.getGrossTotal().divide(totalGross, 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    discountForItem = orderDiscount.multiply(proportion).setScale(2, RoundingMode.HALF_UP);
                    discountConsumed = discountConsumed.add(discountForItem);
                }

                BigDecimal newGross = item.getGrossTotal().subtract(discountForItem);
                
                // Recalculate Net and Tax using Reverse Taxation logic
                BigDecimal taxRateMultiplier = BigDecimal.ONE.add(item.getTaxEntry().getTaxRate().divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
                BigDecimal newNet = newGross.divide(taxRateMultiplier, 2, RoundingMode.HALF_UP);
                BigDecimal newTax = newGross.subtract(newNet);
                
                item.setNetTotal(newNet);
                item.getTaxEntry().setAmount(newTax);
                item.setGrossTotal(newGross);
                
                // Keep track of accumulated discount
                BigDecimal cumulativeItemDiscount = item.getDiscountTotal() != null ? item.getDiscountTotal() : BigDecimal.ZERO;
                item.setDiscountTotal(cumulativeItemDiscount.add(discountForItem));
            }
        }

        // 3. Final Document Accumulation (ensures doc sums match individual line sums exactly)
        for (OrderItem item : items) {
            // Already safely rounded to 2 decimals from calculation above
            orderNetAfterDiscount = orderNetAfterDiscount.add(item.getNetTotal());
            orderGrossTotal = orderGrossTotal.add(item.getGrossTotal());

            if (item.getTaxEntry() != null && item.getTaxEntry().getTaxRate() != null) {
                taxTotals.merge(item.getTaxEntry().getTaxRate(), item.getTaxEntry().getAmount(), BigDecimal::add);
            }
        }
        
        // Convert map to list
        List<TaxEntry> taxTotalist = taxTotals.entrySet().stream()
                .map(e -> new TaxEntry(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return new OrderTotals(orderGrossTotal, orderNetAfterDiscount, orderDiscount, taxTotalist);
    }

    public BigDecimal calculateTax(BigDecimal netPrice, BigDecimal taxRate) {
        return netPrice.multiply(taxRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
    }

    public BigDecimal calculateEUGross(BigDecimal netPrice, BigDecimal taxRate) {
        // Compute line tax and gross () - Gross = Netprice * (1 - taxrate/100)
        BigDecimal taxRateDivisor = taxRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal taxTotal = netPrice.multiply(taxRateDivisor);
        return netPrice.add(taxTotal);
    }

    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(newStatus);
        order.setLatest_update(new Date());

        Order savedOrder = orderRepository.save(order);
        notifyStatusUpdate(savedOrder); // Trigger notification
        return mapToResponse(savedOrder);
    }

    private void notifyStatusUpdate(Order order) {
        OrderResponse response = mapToResponse(order);

        // 1. Specific Order: For the customer's personal tracking screen
        messagingTemplate.convertAndSend("/topic/orders/" + order.getId(), response);

        // 2. Table/Point: For a tablet display at a specific table
        messagingTemplate.convertAndSend("/topic/tables/" + order.getOrderPointId(), response);

        // 3. General Dashboard: For the Kitchen or Waiter "Orders Overview"
        messagingTemplate.convertAndSend("/topic/orders", response);
    }
}
