package repository;

import model.Account;
import model.CheckingAccount;
import model.SavingsAccount;
import util.FileUtil;

import java.util.ArrayList;
import java.util.List;

public class AccountRepository {
    private static final String FILE_PATH = "data/accounts.txt";

    public void save(Account account) {
        FileUtil.appendLine(FILE_PATH, account.toString());
    }

    public List<Account> findAll() {
        List<Account> accounts = new ArrayList<>();
        List<String> lines = FileUtil.readLines(FILE_PATH);
        for (String line : lines) {
            Account acc = parseAccount(line);
            if (acc != null) accounts.add(acc);
        }
        return accounts;
    }

    public Account findById(String accountId) {
        for (Account acc : findAll()) {
            if (acc.getAccountId().equals(accountId)) return acc;
        }
        return null;
    }

    public List<Account> findByCustomerId(String customerId) {
        List<Account> result = new ArrayList<>();
        for (Account acc : findAll()) {
            if (acc.getCustomerId().equals(customerId)) result.add(acc);
        }
        return result;
    }

    public void update(Account account) {
        List<String> lines = FileUtil.readLines(FILE_PATH);
        List<String> updated = new ArrayList<>();
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length > 0 && parts[0].equals(account.getAccountId())) {
                updated.add(account.toString());
            } else {
                updated.add(line);
            }
        }
        FileUtil.writeLines(FILE_PATH, updated);
    }

    private Account parseAccount(String line) {
        String[] parts = line.split(",");
        if (parts.length < 5) return null;
        String accountId = parts[0];
        String customerId = parts[1];
        String type = parts[2];
        double balance = Double.parseDouble(parts[3]);
        java.time.LocalDateTime createdAt = java.time.LocalDateTime.parse(parts[4]);
        
        if (type.equals("SAVINGS")) {
            return new SavingsAccount(accountId, customerId, balance, createdAt, new java.util.ArrayList<>());
        } else if (type.equals("CHECKING")) {
            return new CheckingAccount(accountId, customerId, balance, createdAt, new java.util.ArrayList<>());
        }
        return null;
    }
}
