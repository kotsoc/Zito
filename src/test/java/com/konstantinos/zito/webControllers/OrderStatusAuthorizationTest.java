package com.konstantinos.zito.webControllers;

import com.konstantinos.zito.services.OrderService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableMethodSecurity
class OrderStatusAuthorizationTest {
    @Autowired MockMvc mvc;
    @MockitoBean OrderService service;
    @MockitoBean com.konstantinos.zito.security.JwtTokenFilter tokenFilter;

    @ParameterizedTest
    @CsvSource({"CREATE,ADMIN,201", "CREATE,MANAGER,201", "CREATE,WAITER,201", "CREATE,KITCHEN,403", "CREATE,GUEST,403",
            "EDIT,ADMIN,201", "EDIT,MANAGER,201", "EDIT,WAITER,403", "EDIT,KITCHEN,403", "EDIT,GUEST,403",
            "CANCEL,ADMIN,200", "CANCEL,MANAGER,200", "CANCEL,WAITER,403", "CANCEL,KITCHEN,403", "CANCEL,GUEST,403",
            "VIEW,ADMIN,200", "VIEW,MANAGER,200", "VIEW,WAITER,200", "VIEW,KITCHEN,200", "VIEW,GUEST,403"})
    void orderPermissionMatrix(String action, String role, int expected) throws Exception {
        var authority = new SimpleGrantedAuthority("ROLE_" + role);
        var principal = new com.konstantinos.zito.security.RestaurantUserDetails(UUID.randomUUID(),
                "staff@example.com", List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, "", List.of(authority)));
        when(service.createOrder(any(), any())).thenReturn(new com.konstantinos.zito.model.dto.OrderResponse());
        when(service.updateOrder(any(), any(), any())).thenReturn(new com.konstantinos.zito.model.dto.OrderResponse());
        when(service.cancelOrder(any(), any(), any(), any())).thenReturn(new com.konstantinos.zito.model.dto.OrderResponse());
        String id = UUID.randomUUID().toString();
        var request = switch (action) {
            case "CREATE" -> post("/api/v1/orders/waiter");
            case "EDIT" -> put("/api/v1/orders/waiter/" + id);
            case "CANCEL" -> post("/api/v1/orders/" + id + "/cancellation");
            default -> get("/api/v1/orders");
        };
        String body = "{\"version\":0,\"cartId\":\"" + id
                + "\",\"orderPointId\":\"table-1\",\"discountPercent\":10,\"cartItems\":[{\"menuItemId\":\"item\",\"quantity\":1}]}";
        if (action.equals("CANCEL")) body = "{\"version\":0,\"reason\":\"customer request\"}";
        try {
            mvc.perform(request.contentType("application/json").content(body)).andExpect(status().is(expected));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @ParameterizedTest
    @CsvSource({"KITCHEN,PREPARING,200", "KITCHEN,READY,200", "KITCHEN,CONFIRMED,403",
            "KITCHEN,COMPLETED,403", "GUEST,PREPARING,403", "ADMIN,CONFIRMED,200",
            "MANAGER,CONFIRMED,200", "WAITER,COMPLETED,200"})
    void statusPermissions(String role, String target, int expected) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "staff", "", List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        try {
            mvc.perform(patch("/api/v1/orders/{id}/status", UUID.randomUUID())
                    .contentType("application/json")
                    .content("{\"version\":0,\"status\":\"" + target + "\"}"))
                    .andExpect(status().is(expected));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
