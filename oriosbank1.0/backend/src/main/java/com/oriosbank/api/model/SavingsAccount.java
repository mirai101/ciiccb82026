package com.oriosbank.api.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class SavingsAccount extends Account {

    private static final double INTEREST_RATE = 0.025;
    private static final double MINIMUM_BALANCE = 100.0;

    @Override
    public String getAccountType() {
        return "SAVINGS";
    }

    @Override
    public double getInterestRate() {
        return INTEREST_RATE;
    }

    public double getMinimumBalance() {
        return MINIMUM_BALANCE;
    }
}
