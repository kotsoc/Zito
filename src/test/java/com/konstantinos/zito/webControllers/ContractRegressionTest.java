package com.konstantinos.zito.webControllers;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
    void categoryRoutesDispatchByIdAndName() throws Exception {
        MenuService service = mock(MenuService.class);
        when(service.findCategoryById(42)).thenReturn(Optional.empty());
        when(service.findCategory("42")).thenReturn(Optional.empty());
        var mvc = MockMvcBuilders.standaloneSetup(new CategoryController(service)).build();

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
