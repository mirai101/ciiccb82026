package com.moocash.api.model;

import lombok.*;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @Column(name = "loan_id", length = 36)
    private String loanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "remaining_balance", precision = 19, scale = 2)
    private BigDecimal remainingBalance;

    @Column(name = "interest_rate", precision = 9, scale = 4)
    private BigDecimal interestRate;

    @Builder.Default
    private String status = "PENDING";

    @Builder.Default
    @Column(name = "auto_debt_enabled")
    private boolean autoDebtEnabled = false;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
