package service;

import model.Account;
import model.Transaction;
import repository.AccountRepository;
import repository.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

public class TransactionService {
    private final AccountRepository accountRepo;
    private final TransactionRepository transactionRepo;

    public TransactionService() {
        this.accountRepo = new AccountRepository();
        this.transactionRepo = new TransactionRepository();
    }

    public List<Transaction> getAccountTransactions(String accountId) {
        return transactionRepo.findByAccountId(accountId);
    }

    public List<Transaction> getAllCustomerTransactions(String customerId) {
        List<Transaction> all = new ArrayList<>();
        for (Account acc : accountRepo.findByCustomerId(customerId)) {
            all.addAll(transactionRepo.findByAccountId(acc.getAccountId()));
        }
        return all;
    }
}
