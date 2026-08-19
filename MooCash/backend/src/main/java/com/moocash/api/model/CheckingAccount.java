package com.moocash.api.model;

import lombok.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "checking_accounts")
@Getter
@Setter
@NoArgsConstructor
public class CheckingAccount extends Account {

    private static final BigDecimal INTEREST_RATE = new BigDecimal("0.005");
    private static final BigDecimal OVERDRAFT_LIMIT = new BigDecimal("500.00");
    public static final BigDecimal MAX_BALANCE = new BigDecimal("100000000.00");

    @Override
    public String getAccountType() {
        return "CHECKING";
    }

    @Override
    public BigDecimal getInterestRate() {
        return INTEREST_RATE;
    }

    public BigDecimal getOverdraftLimit() {
        return OVERDRAFT_LIMIT;
    }
}
