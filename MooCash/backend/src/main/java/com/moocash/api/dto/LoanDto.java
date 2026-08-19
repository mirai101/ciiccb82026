package com.moocash.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanDto {
    private String loanId;
    private String customerId;
    private String customerName;
    private BigDecimal amount;
    private BigDecimal remainingBalance;
    private BigDecimal interestRate;
    private String status;
    private boolean autoDebtEnabled;
    private LocalDateTime createdAt;
}
