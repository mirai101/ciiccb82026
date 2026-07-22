package com.oriosbank.api.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDto {

    @NotBlank(message = "From account ID is required")
    private String fromAccountId;

    @NotBlank(message = "To account ID is required")
    private String toAccountId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    private String description;
}
