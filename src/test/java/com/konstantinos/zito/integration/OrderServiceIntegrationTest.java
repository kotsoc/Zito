package com.konstantinos.zito.integration;

import com.konstantinos.zito.model.*;
import com.konstantinos.zito.model.dto.*;
import com.konstantinos.zito.repositories.MenuItemRepository;
import com.konstantinos.zito.repositories.OrderRepository;
import com.konstantinos.zito.repositories.UserRepository;
import com.konstantinos.zito.services.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThat;

@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest
 {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired private OrderService orderService;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void setup() {
        orderRepository.deleteAll();
        menuItemRepository.deleteAll();

    }

    @Test
    void caseA_shouldCorrectlyExtractVATForMultipleRatesAfterDiscount() {
        // GIVEN: 3 items with 3 different EU rates (Net prices)
        String pizzaId = createEUItem("Pizza", "15.00", "10.0");
        String wineId = createEUItem("Wine", "40.00", "24.0");
        String sodaId = createEUItem("Soda", "4.50", "13.0");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setDiscountPercent(BigDecimal.valueOf(10.0)); // 10% Off
        request.setCartItems(List.of(
            new CartItemRequest(pizzaId, 1, "null",List.of() ,0),
            new CartItemRequest(wineId, 1, "null",List.of() ,0),
            new CartItemRequest(sodaId,1, "null",List.of() ,0)
        ));

        // WHEN
        RestaurantUser waiter = new RestaurantUser("discount-waiter@example.com");
        waiter.addRole(Role.WAITER.getAuthority());
        userRepository.save(waiter);
        OrderResponse response = orderService.createOrder(waiter.getId(), request);
        Order savedOrder = orderRepository.findById(response.getId()).get();

        // THEN: Total Net = (15+40+4.50)59.5 * 0.9 = 53.55
        assertThat(savedOrder.getNetTotal()).isEqualByComparingTo("53.55");

        // THEN: Verify the VAT Breakdown
        // 10% Rate: 15.00 * 0.9 = 13.50. VAT = 13.50 *0.1 = 1.35
        assertTaxAtRate(savedOrder, "10.0", "1.35");

        // 24% Rate: 40.00 * 0.9 = 36.00. VAT = 36.00 * 0.24 = 8.64
        assertTaxAtRate(savedOrder, "24.0", "8.64");

        // 13% Rate: 4.50 * 0.9 = 4.05  4.05 × 0.13 = 0.5265 → 0.53
        assertTaxAtRate(savedOrder, "13.0", "0.53");

        // Gross = 53.55 + (1.35 + 8.64 + 0.53)
        assertThat(savedOrder.getGrossTotal()).isEqualByComparingTo("64.07");
    }

    @Test
    void caseB_shouldHandleQuantityRoundingWithoutLosingCents() {
        // GIVEN: Item at €2.99 net (10% VAT)
        String snackId = createEUItem("Snack", "2.99", "10.0");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setDiscountPercent(BigDecimal.valueOf(0.0));
        request.setCartItems(List.of(new CartItemRequest(snackId, 3, "null", List.of(), 0)));

        // WHEN
        OrderResponse response = orderService.createGuestOrder(request);
        Order savedOrder = orderRepository.findById(response.getId()).get();

        // THEN: Net = 2.99×3 = 8.97
        // VAT 10% = 8.97×0.10 = 0.897 → 0.90
        // Gross = 8.97 + 0.90 = 9.87
        assertThat(savedOrder.getGrossTotal()).isEqualByComparingTo("9.87");
        assertTaxAtRate(savedOrder, "10.0", "0.90");
    }


    private String createEUItem(String name, String price, String rate) {
        MenuItem item = new MenuItem();
        String id = name+123+"id";
        item.setId(id);
        item.setName(name);
        item.setNetPrice(new BigDecimal(price));
        item.setTaxRate(new BigDecimal(rate));
        item.setAvailable(true);
        item.setCategoryName("burg");
        return menuItemRepository.save(item).getId();
    }

    private void assertTaxAtRate(Order order, String rateStr, String expectedTax) {
        var taxTotal = order.getTaxTotals().stream().filter(e -> e.getTaxRate().toString().equals(rateStr)).findAny();
        assertThat(expectedTax)
            .as("VAT for rate " + rateStr)
            .isEqualTo(taxTotal.get().getAmount().toString());
    }
}