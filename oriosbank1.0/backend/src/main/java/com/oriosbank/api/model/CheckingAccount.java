package com.oriosbank.api.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class CheckingAccount extends Account {

    private static final double INTEREST_RATE = 0.005;
    private static final double OVERDRAFT_LIMIT = 500.0;

    @Override
    public String getAccountType() {
        return "CHECKING";
    }

    @Override
    public double getInterestRate() {
        return INTEREST_RATE;
    }

    public double getOverdraftLimit() {
        return OVERDRAFT_LIMIT;
    }
}
