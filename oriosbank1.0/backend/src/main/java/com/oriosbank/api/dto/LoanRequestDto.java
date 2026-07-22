package com.oriosbank.api.dto;

import jakarta.validation.constraints.Min;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRequestDto {
    @Min(value = 100, message = "Minimum loan amount is 100")
    private Double amount;
    
    @Min(value = 0, message = "Interest rate cannot be negative")
    private Double interestRate;
}
