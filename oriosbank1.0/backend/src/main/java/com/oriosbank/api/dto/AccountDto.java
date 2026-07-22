package com.oriosbank.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {
    private String accountId;
    private String customerId;

    @NotBlank(message = "Account type is required")
    @Pattern(regexp = "^(SAVINGS|CHECKING)$", message = "Type must be SAVINGS or CHECKING")
    private String type;

    @NotNull(message = "Balance is required")
    @Min(value = 0, message = "Balance cannot be negative")
    private Double balance;

    private Double interestRate;
    private String status;
    private boolean isHidden;
    private LocalDateTime createdAt;
}
