package com.konstantinos.zito.model.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mongodb.lang.Nullable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateOrderRequest {
    @NotNull
    UUID cartId; // reference to the shopping cart FE generated

    @NotBlank
    String orderPointId; // e.g. table number or reservation ID

    @Size(min = 1, message = "Cart must have at least one item")
    List<CartItemRequest> cartItems;

    @Email
    @Nullable
    String guestEmail;

    String currency;
    String customerNote;
    BigDecimal discountPercent;
}
