package model;

public class SavingsAccount extends Account {
    private static final double INTEREST_RATE = 0.025;
    private static final double MINIMUM_BALANCE = 100.0;

    public SavingsAccount(String customerId, double initialBalance) {
        super(customerId, initialBalance);
    }

    public SavingsAccount(String accountId, String customerId, double balance, java.time.LocalDateTime createdAt, java.util.List<Transaction> transactions) {
        super(accountId, customerId, balance, createdAt, transactions);
    }

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

    @Override
    public void withdraw(double amount) throws exception.InsufficientBalanceException, exception.InvalidAmountException {
        if (balance - amount < MINIMUM_BALANCE) {
            throw new exception.InsufficientBalanceException("Minimum balance of $" + MINIMUM_BALANCE + " must be maintained");
        }
        super.withdraw(amount);
    }
}
