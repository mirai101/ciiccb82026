package com.moocash.api.model;

import lombok.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "savings_accounts")
@Getter
@Setter
@NoArgsConstructor
public class SavingsAccount extends Account {

    private static final BigDecimal INTEREST_RATE = new BigDecimal("0.025");
    private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("100.00");
    public static final BigDecimal MAX_BALANCE = new BigDecimal("100000000.00");

    @Override
    public String getAccountType() {
        return "SAVINGS";
    }

    @Override
    public BigDecimal getInterestRate() {
        return INTEREST_RATE;
    }

    public BigDecimal getMinimumBalance() {
        return MINIMUM_BALANCE;
    }
}
