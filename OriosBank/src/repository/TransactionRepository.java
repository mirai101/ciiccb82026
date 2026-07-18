package repository;

import model.Transaction;
import util.FileUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionRepository {
    private static final String FILE_PATH = "data/transactions.txt";

    public void save(Transaction transaction) {
        FileUtil.appendLine(FILE_PATH, transaction.toString());
    }

    public List<Transaction> findAll() {
        List<Transaction> transactions = new ArrayList<>();
        List<String> lines = FileUtil.readLines(FILE_PATH);
        for (String line : lines) {
            Transaction t = parseTransaction(line);
            if (t != null) transactions.add(t);
        }
        return transactions;
    }

    public List<Transaction> findByAccountId(String accountId) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : findAll()) {
            if (accountId.equals(t.getFromAccount()) || accountId.equals(t.getToAccount())) {
                result.add(t);
            }
        }
        return result;
    }

    private Transaction parseTransaction(String line) {
        String[] parts = line.split(",");
        if (parts.length < 4) return null;
        try {
            String id = parts[0];
            String type = parts[1];
            double amount = Double.parseDouble(parts[2]);
            LocalDateTime timestamp = LocalDateTime.parse(parts[3]);
            String from = parts.length > 4 && !parts[4].equals("null") ? parts[4] : null;
            String to = parts.length > 5 && !parts[5].equals("null") ? parts[5] : null;

            return new Transaction(id, type, amount, timestamp, from, to);
        } catch (Exception e) {
            return null;
        }
    }
}
