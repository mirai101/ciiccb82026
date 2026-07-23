package com.oriosbank.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRequestDto {
    @Min(value = 100, message = "Minimum loan amount is 100")
    @Max(value = 2000000, message = "Maximum loan amount is 2,000,000")
    private Double amount;

    @Min(value = 0, message = "Interest rate cannot be negative")
    private Double interestRate;
}
