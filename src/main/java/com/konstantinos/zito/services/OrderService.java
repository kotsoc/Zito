package com.konstantinos.zito.services;

import org.springframework.data.mongodb.core.aggregation.AccumulatorOperators.Sum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.konstantinos.zito.model.dto.OrderResponse;
import com.konstantinos.zito.model.dto.CreateOrderRequest;
import com.konstantinos.zito.model.dto.CartItemRequest;
import com.konstantinos.zito.model.dto.OrderItemResponse;

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
        order.setOrderItems(calculateCartSnapshot(request.getCartItems()));
        order.setPaid(false);
        order.setStatus(OrderStatus.NEW);
        var orderTotals = calculateOrderTotals(order.getOrderItems(), order.getDiscountTotal());
        order.setNetTotal(orderTotals.getNetTotal());
        order.setTaxTotals(orderTotals.getTaxTotalsByRate());
        order.setGrossTotal(orderTotals.getGrossTotal());

        // email is optional
        if (request.getGuestEmail() != null) {
            order.setGuestEmail(request.getGuestEmail());
        }

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
        order.setOrderItems(calculateCartSnapshot(request.getCartItems()));

        order.setPaid(false); // default
        order.setStatus(OrderStatus.NEW); // default or NEW
        var orderTotals = calculateOrderTotals(order.getOrderItems(), request.getDiscount());
        order.setGrossTotal(orderTotals.getGrossTotal());
        order.setTaxTotals(orderTotals.getTaxTotalsByRate());
        order.setNetTotal(orderTotals.getNetTotal());
        order.setDiscountTotal(orderTotals.getDiscountTotal());

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

        existingOrder.setOrderItems(calculateCartSnapshot(request.getCartItems()));
        existingOrder.setLatest_update(new Date());
        existingOrder.setOrderPointId(request.getOrderPointId());

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
        response.setWaiterId(order.getWaiterId());
        response.setOrderPointId(order.getOrderPointId());
        response.setCreatedAt(order.getCreated_At());
        response.setLatestUpdate(order.getLatest_update());
        response.setPaid(order.isPaid());
        response.setOrderItems(order.getOrderItems().stream().map(ci -> {
            var ciResp = new OrderItemResponse();
            ciResp.setMenuItemId(ci.getMenuItemId());
            ciResp.setName(ci.getName());
            ciResp.setUnitPrice(ci.getUnitPriceGross());
            ciResp.setQuantity(ci.getQuantity());
            ciResp.setDiscount(ci.getItemDiscount());
            ciResp.setNote(ci.getNote());
            ciResp.setModifiers(ci.getItemModifiers());
            ciResp.setTax(ci.getTaxRate());
            ciResp.setSubtotal(ci.getGrossTotal());
            return ciResp;
        }).toList());
        response.setSubTotal(order.getNetTotal());
        response.setGrandTotal(order.getGrossTotal());
        Map<String, BigDecimal> formattedTaxes = new HashMap<>();
        order.getTaxTotals().forEach((rate, amount) -> {
            // Normalize the rate to "10.0", "24.0", etc.
            formattedTaxes.put(rate.stripTrailingZeros().toPlainString(), amount);
        });
        response.setTaxTotals(formattedTaxes);
        return response;
    }


    // Item level discount - For each item, calculate the gross price after any item discount.
    // Discounted Net Price = Net Price × (1 – Item Discount %)
    // VAT = Discounted Net Price × VAT Rate
    // Gross Price = Discounted Net Price + VAT
    private List<OrderItem> calculateCartSnapshot(List<CartItemRequest> cartItems) {
        return cartItems.stream().map(ciReq -> {
            MenuItem menuItem = menuItemRepository.findById(ciReq.getMenuItemId())
                    .orElseThrow(() -> new RuntimeException("Menu item not found: " + ciReq.getMenuItemId()));
            
            // Set base information
            OrderItem item = new OrderItem();
            item.setMenuItemId(menuItem.getId());
            item.setName(menuItem.getName());
            item.setQuantity(ciReq.getQuantity());
            item.setNote(ciReq.getNote());
            item.setItemModifiers(ciReq.getModifiers());// TODO :FIX modifiers pricing
            item.setItemDiscount(
                    ciReq.getItemDiscount() != 0.0d ? BigDecimal.valueOf(ciReq.getItemDiscount()) : BigDecimal.ZERO);

            // 1. Initial Pricing Snapshots assuming EU Prices
            BigDecimal netPrice = menuItem.getNetPrice();

            // 2. Line net total
            BigDecimal lineNetTotal = netPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            // 3. Apply item discount to line and save
            BigDecimal itemDiscountAmount = lineNetTotal.multiply(item.getItemDiscount())
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP); // high precision for intermediate calc
            BigDecimal lineNetAfterItemDiscount = lineNetTotal.subtract(itemDiscountAmount);
            item.setDiscountTotal(itemDiscountAmount);

            // 4. Compute line tax and gross () - Gross = Netprice * (1 - taxrate/100)
            BigDecimal lineTax = calculateTax(lineNetAfterItemDiscount, menuItem.getTaxRate());
            BigDecimal lineGross = lineNetAfterItemDiscount.add(lineTax);
            
            var taxEntry = new TaxEntry(menuItem.getTaxRate(), lineTax);
            item.setNetTotal(lineNetAfterItemDiscount);
            item.setGrossTotal(lineGross);

            return item;
        }).toList();
    }

    // Order-Level Calculation - Sum all item gross prices, then apply the order discount.
    // Subtotal Gross = Sum of all item gross prices
    // Order Discount = Subtotal Gross × Order Discount %
    // Final Gross Total = Subtotal Gross – Order Discount
    public OrderTotals calculateOrderTotals(List<OrderItem> items, BigDecimal discountRate) {
        BigDecimal orderNetTotal = BigDecimal.ZERO;
        BigDecimal orderGrossTotal = BigDecimal.ZERO;
        Map<BigDecimal, BigDecimal> taxTotals = new HashMap<>();

        BigDecimal totalNet = items.stream()
                .map(OrderItem::getNetTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal orderDiscount = BigDecimal.ZERO;
        if (discountRate != null && discountRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalOrderDiscount = totalNet.multiply(discountRate)
                    .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

            items.forEach(item -> {
                BigDecimal proportion = item.getNetTotal().divide(totalNet, 10, RoundingMode.HALF_UP);
                BigDecimal discountForItem = totalOrderDiscount.multiply(proportion);

                BigDecimal newNet = item.getNetTotal().subtract(discountForItem);
                BigDecimal newTax = calculateTax(newNet, item.getTaxRate().getTaxRate());
                BigDecimal newGross = newNet.add(newTax);

                item.setNetTotal(newNet.setScale(2, RoundingMode.HALF_UP));
                item.getTaxRate().setAmount(newTax);;
                item.setGrossTotal(newGross.setScale(2, RoundingMode.HALF_UP));
                item.setDiscountTotal(discountForItem.setScale(2, RoundingMode.HALF_UP));
            });
            // Needed due to values inside lambda needing to be final
            orderDiscount = totalOrderDiscount;
        }

        for (OrderItem item : items) {
            orderNetTotal = orderNetTotal.add(item.getNetTotal());
            orderGrossTotal = orderGrossTotal.add(item.getGrossTotal());

            if (item.getTaxRate() != null && item.getTaxRate().getTaxRate() != null) {
                taxTotals.merge(item.getTaxRate().getTaxRate(), item.getTaxRate().getAmount(), BigDecimal::add);
            }
        }
        // 7️⃣ Build TaxTotal list (round at the end)
        List<TaxEntry> taxTotalist = taxTotals.entrySet().stream()
                .map(e -> new TaxEntry(e.getKey(), e.getValue().setScale(2, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());

        return new OrderTotals(orderGrossTotal, orderNetTotal, orderDiscount, taxTotals);
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
}