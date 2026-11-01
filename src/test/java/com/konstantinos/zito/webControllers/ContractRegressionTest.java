package com.konstantinos.zito.webControllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.konstantinos.zito.model.dto.CartItemRequest;
import com.konstantinos.zito.model.dto.CreateOrderRequest;
import com.konstantinos.zito.model.dto.OrderStatusRequest;
import com.konstantinos.zito.services.MenuService;

import jakarta.validation.Validation;

class ContractRegressionTest {
    @Test
    void invalidNestedInputIs400AndDatabaseFailureIs503() throws Exception {
        var service = mock(com.konstantinos.zito.services.OrderService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new OrderController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        String body = "{\"cartId\":\"" + UUID.randomUUID() + "\",\"orderPointId\":\"table\","
                + "\"cartItems\":[{\"menuItemId\":\"item\",\"quantity\":0}]}";
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/orders")
                .contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(service);
        when(service.createGuestOrder(any())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("unavailable"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/orders")
                .contentType("application/json").content(body.replace("\"quantity\":0", "\"quantity\":1")))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"));
    }
    @Test
    void statusConflictsUseCommonErrorAndMalformedRequestsReturn400() throws Exception {
        var service = mock(com.konstantinos.zito.services.OrderService.class);
        UUID id = UUID.randomUUID();
        when(service.updateOrderStatus(id, com.konstantinos.zito.model.OrderStatus.CONFIRMED, 0L))
                .thenThrow(new org.springframework.dao.OptimisticLockingFailureException("stale"));
        var mvc = MockMvcBuilders.standaloneSetup(new OrderController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(patch("/api/v1/orders/{id}/status", id).contentType("application/json")
                .content("{\"version\":0,\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
        mvc.perform(patch("/api/v1/orders/{id}/status", id).contentType("application/json")
                .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
    @Test
    void categoryRoutesDispatchByIdAndName() throws Exception {
        MenuService service = mock(MenuService.class);
        when(service.findCategoryById(42)).thenReturn(Optional.empty());
        when(service.findCategory("42")).thenReturn(Optional.empty());
        var mvc = MockMvcBuilders.standaloneSetup(new CategoryController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();

        mvc.perform(get("/api/v1/categories/42")).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/categories/by-name/42")).andExpect(status().isNotFound());
        verify(service).findCategoryById(42);
        verify(service).findCategory("42");
    }

    @Test
    void nestedCartAndStatusAreValidated() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var request = new CreateOrderRequest();
            request.setCartId(UUID.randomUUID());
            request.setOrderPointId("table");
            assertFalse(validator.validate(request).isEmpty());
            request.setCartItems(List.of(new CartItemRequest()));
            assertFalse(validator.validate(request).isEmpty());
            request.setCartItems(java.util.Collections.singletonList(null));
            assertFalse(validator.validate(request).isEmpty());
            assertFalse(validator.validate(new OrderStatusRequest()).isEmpty());
        }
    }
}
