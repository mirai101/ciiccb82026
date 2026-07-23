package com.oriosbank.api.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    private String loanId;

    @DBRef
    private Customer customer;

    private Double amount;

    private Double remainingBalance;

    private Double interestRate;

    @Builder.Default
    private String status = "PENDING";

    @Builder.Default
    private boolean autoDebtEnabled = false;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;
}
