package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class Transaction implements Serializable {
    private String transactionId;
    private String type;
    private double amount;
    private LocalDateTime timestamp;
    private String fromAccount;
    private String toAccount;

    public Transaction(String type, double amount, String fromAccount, String toAccount) {
        this(UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
             type, amount, LocalDateTime.now(), fromAccount, toAccount);
    }

    public Transaction(String transactionId, String type, double amount, LocalDateTime timestamp, String fromAccount, String toAccount) {
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
    }

    // Getters
    public String getTransactionId() { return transactionId; }
    public String getType() { return type; }
    public double getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getFromAccount() { return fromAccount; }
    public String getToAccount() { return toAccount; }

    @Override
    public String toString() {
        return transactionId + "," + type + "," + amount + "," + timestamp + "," + fromAccount + "," + toAccount;
    }
}
