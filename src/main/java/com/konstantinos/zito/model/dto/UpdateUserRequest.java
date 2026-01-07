package com.konstantinos.zito.model.dto;

import java.util.Optional;
import java.util.Set;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    String phoneNumber;

    @NotBlank
    @Size(min = 6, max = 45, message = "Email must be between 6 and 45 characters")
    @Email
    String email;
    
    @Size(min = 1, message = "At least one role must be specified")
    Set<String> roles;
}
