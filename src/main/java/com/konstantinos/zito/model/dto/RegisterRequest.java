package com.konstantinos.zito.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    
    @Size(min = 6, max = 45, message = "Email must be between 6 and 45 characters")
    @Email
    String email;
    
    @Size(min = 5, max = 25, message = "Password must be between 5 and 25 characters")
    @NotBlank
    String password;
    String phoneNumber;
}
