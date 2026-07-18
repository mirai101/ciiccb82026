package model;

public class CheckingAccount extends Account {
    private static final double INTEREST_RATE = 0.005;
    private static final double OVERDRAFT_LIMIT = 500.0;

    public CheckingAccount(String customerId, double initialBalance) {
        super(customerId, initialBalance);
    }

    public CheckingAccount(String accountId, String customerId, double balance, java.time.LocalDateTime createdAt, java.util.List<Transaction> transactions) {
        super(accountId, customerId, balance, createdAt, transactions);
    }

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

    @Override
    public void withdraw(double amount) throws exception.InsufficientBalanceException, exception.InvalidAmountException {
        if (amount <= 0) throw new exception.InvalidAmountException("Amount must be positive");
        if (amount > balance + OVERDRAFT_LIMIT) {
            throw new exception.InsufficientBalanceException("Overdraft limit exceeded. Limit: $" + OVERDRAFT_LIMIT);
        }
        balance -= amount;
        transactions.add(new Transaction("WITHDRAW", amount, accountId, null));
    }
}
