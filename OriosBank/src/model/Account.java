package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Account implements Serializable {
    protected String accountId;
    protected String customerId;
    protected double balance;
    protected LocalDateTime createdAt;
    protected List<Transaction> transactions;

    public Account(String customerId, double initialBalance) {
        this(UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
             customerId, initialBalance, LocalDateTime.now(), new ArrayList<>());
    }

    public Account(String accountId, String customerId, double balance, LocalDateTime createdAt, List<Transaction> transactions) {
        this.accountId = accountId;
        this.customerId = customerId;
        this.balance = balance;
        this.createdAt = createdAt;
        this.transactions = transactions != null ? transactions : new ArrayList<>();
    }

    public abstract String getAccountType();
    public abstract double getInterestRate();

    public void deposit(double amount) {
        balance += amount;
        transactions.add(new Transaction("DEPOSIT", amount, accountId, null));
    }

    public void withdraw(double amount) throws exception.InsufficientBalanceException, exception.InvalidAmountException {
        if (amount <= 0) throw new exception.InvalidAmountException("Amount must be positive");
        if (amount > balance) throw new exception.InsufficientBalanceException("Insufficient balance");
        balance -= amount;
        transactions.add(new Transaction("WITHDRAW", amount, accountId, null));
    }

    public void transferTo(Account target, double amount) throws exception.InsufficientBalanceException, exception.InvalidAmountException {
        if (amount <= 0) throw new exception.InvalidAmountException("Amount must be positive");
        if (amount > balance) throw new exception.InsufficientBalanceException("Insufficient balance");
        this.withdraw(amount);
        target.deposit(amount);
        transactions.add(new Transaction("TRANSFER_OUT", amount, accountId, target.getAccountId()));
        target.getTransactions().add(new Transaction("TRANSFER_IN", amount, target.getAccountId(), accountId));
    }

    // Getters and Setters
    public String getAccountId() { return accountId; }
    public String getCustomerId() { return customerId; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Transaction> getTransactions() { return transactions; }

    @Override
    public String toString() {
        return accountId + "," + customerId + "," + getAccountType() + "," + balance + "," + createdAt;
    }
}
