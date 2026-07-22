package com.oriosbank.api.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanDto {
    private String loanId;
    private String customerId;
    private String customerName;
    private Double amount;
    private Double remainingBalance;
    private Double interestRate;
    private String status;
    private boolean autoDebtEnabled;
    private LocalDateTime createdAt;
}
