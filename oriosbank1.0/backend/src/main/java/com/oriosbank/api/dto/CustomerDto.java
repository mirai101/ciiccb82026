package com.oriosbank.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
    private String customerId;

    @NotBlank(message = "Full name is required")
    @Pattern(regexp = "^[A-Za-z\\s.'-]+$", message = "Name must contain only letters, spaces, dots, hyphens, and apostrophes")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @Pattern(regexp = "^0[0-9]{10}$", message = "Phone number must be exactly 11 digits starting with 0")
    private String phone;

    private String role;

    private LocalDateTime registeredAt;
}
