package com.moocash.api.model;

import lombok.*;
import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SuperBuilder
@Entity
@Table(name = "accounts")
@Inheritance(strategy = InheritanceType.JOINED)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Account {

    @Id
    @Column(name = "account_id", length = 36)
    private String accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // BigDecimal, not double/Double: currency values must never use binary
    // floating point, which cannot represent amounts like 0.1 exactly and
    // accumulates rounding error across many transactions.
    @Column(name = "balance", precision = 19, scale = 2)
    private BigDecimal balance;

    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "is_hidden")
    private boolean isHidden = false;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "account", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Transaction> transactions = new ArrayList<>();

    @Version
    private Long version;

    public abstract String getAccountType();
    public abstract BigDecimal getInterestRate();
}
