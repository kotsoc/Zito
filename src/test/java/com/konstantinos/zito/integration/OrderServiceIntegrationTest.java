package com.konstantinos.zito.integration;

import com.konstantinos.zito.model.*;
import com.konstantinos.zito.model.dto.*;
import com.konstantinos.zito.repositories.MenuItemRepository;
import com.konstantinos.zito.repositories.OrderRepository;
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

@SpringBootTest
@Testcontainers
class OrderServiceEUTaxTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:6.0");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired private OrderService orderService;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private OrderRepository orderRepository;

    @BeforeEach
    void setup() {
        orderRepository.deleteAll();
        menuItemRepository.deleteAll();
    }

    @Test
    void caseA_shouldCorrectlyExtractVATForMultipleRatesAfterDiscount() {
        // GIVEN: 3 items with 3 different EU rates (All inclusive)
        String pizzaId = createEUItem("Pizza", "15.00", "10.0");
        String wineId = createEUItem("Wine", "40.00", "24.0");
        String sodaId = createEUItem("Soda", "4.50", "13.0");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setDiscount(BigDecimal.valueOf(10.0)); // 10% Off
        request.setCartItems(List.of(
            new CartItemRequest(pizzaId, 1, "null",List.of() ,7.5d),
            new CartItemRequest(wineId, 1, "null",List.of() ,0.0d),
            new CartItemRequest(sodaId,1, "null",List.of() ,7.5d)
        ));

        // WHEN
        OrderResponse response = orderService.createGuestOrder(request);
        Order savedOrder = orderRepository.findById(response.getId()).get();

        // THEN: Total Gross = (15+40+4.50) * 0.9 = 53.55
        assertThat(savedOrder.getGrossTotal()).isEqualByComparingTo("53.55");

        // THEN: Verify the VAT Breakdown
        // 10% Rate: 15.00 * 0.9 = 13.50. VAT = 13.50 - (13.50/1.1) = 1.227 -> 1.23
        assertTaxAtRate(savedOrder, "10.0", "1.23");

        // 24% Rate: 40.00 * 0.9 = 36.00. VAT = 36.00 - (36.00/1.24) = 6.967 -> 6.97
        assertTaxAtRate(savedOrder, "24.0", "6.97");

        // 13% Rate: 4.50 * 0.9 = 4.05. VAT = 4.05 - (4.05/1.13) = 0.465 -> 0.47
        assertTaxAtRate(savedOrder, "13.0", "0.47");
    }

    @Test
    void caseB_shouldHandleQuantityRoundingWithoutLosingCents() {
        // GIVEN: 3 small items at €2.99 each (10% VAT)
        String snackId = createEUItem("Snack", "2.99", "10.0");

        CreateOrderRequest request = new CreateOrderRequest();
        request.setDiscount(BigDecimal.valueOf(0.0)); 
        request.setCartItems(List.of( new CartItemRequest(snackId,3, "null",List.of() ,0.0)));

        // WHEN
        OrderResponse response = orderService.createGuestOrder(request);
        Order savedOrder = orderRepository.findById(response.getId()).get();

        // THEN: Gross = 2.99 * 3 = 8.97
        assertThat(savedOrder.getGrossTotal()).isEqualByComparingTo("8.97");

        // THEN: Tax Extraction (8.97 / 1.1 = 8.1545. VAT = 8.97 - 8.15 = 0.82)
        assertTaxAtRate(savedOrder, "10.0", "0.82");
    }

    private String createEUItem(String name, String price, String rate) {
        MenuItem item = new MenuItem();
        String id = "name"+123+"id";
        item.setId(id);
        item.setName(name);
        item.setNetPrice(BigDecimal.valueOf(Double.parseDouble(price)));
        item.setTaxRate(new BigDecimal(rate));
        item.setAvailable(true);
        item.setCategoryName("burg");
        return menuItemRepository.save(item).getId();
    }

    private void assertTaxAtRate(Order order, String rateStr, String expected) {
        BigDecimal rate = BigDecimal.valueOf(Double.parseDouble(rateStr));
        assertThat(order.getTaxTotals().get(rate))
            .as("VAT for rate " + rateStr)
            .isEqualByComparingTo(expected);
    }
}