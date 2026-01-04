package com.konstantinos.zito.model.dto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateOrderRequest {
    @NotBlank
    UUID cartId; // reference to the shopping cart FE generated

    @NotBlank
    String orderPointId; // e.g. table number or reservation ID

    @Min(1)
    List<CartItemRequest> cartItems;

    @Email
    Optional<String> guestEmail;

    String currency;
    String customerNote;
}
