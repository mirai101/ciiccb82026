package com.oriosbank.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {
    private String transactionId;

    @NotBlank(message = "Transaction type is required")
    private String type;

    @NotNull(message = "Amount is required")
    @Min(value = 0, message = "Amount must be positive")
    private Double amount;

    private LocalDateTime timestamp;
    private String fromAccount;
    private String toAccount;
    private String accountId;
    private String description;
}
