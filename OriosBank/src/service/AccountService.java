package service;

import model.Account;
import repository.AccountRepository;
import repository.TransactionRepository;
import exception.InsufficientBalanceException;
import exception.InvalidAmountException;
import model.Transaction;

public class AccountService {
    private final AccountRepository accountRepo;
    private final TransactionRepository transactionRepo;

    public AccountService() {
        this.accountRepo = new AccountRepository();
        this.transactionRepo = new TransactionRepository();
    }

    public void deposit(String accountId, double amount) throws InvalidAmountException {
        if (amount <= 0) throw new InvalidAmountException("Amount must be positive");
        Account acc = accountRepo.findById(accountId);
        if (acc == null) throw new IllegalArgumentException("Account not found");
        acc.deposit(amount);
        accountRepo.update(acc);
        transactionRepo.save(new Transaction("DEPOSIT", amount, accountId, null));
    }

    public void withdraw(String accountId, double amount) throws InsufficientBalanceException, InvalidAmountException {
        Account acc = accountRepo.findById(accountId);
        if (acc == null) throw new IllegalArgumentException("Account not found");
        acc.withdraw(amount);
        accountRepo.update(acc);
        transactionRepo.save(new Transaction("WITHDRAW", amount, accountId, null));
    }

    public void transfer(String fromId, String toId, double amount) throws InsufficientBalanceException, InvalidAmountException {
        Account from = accountRepo.findById(fromId);
        Account to = accountRepo.findById(toId);
        if (from == null || to == null) throw new IllegalArgumentException("Account not found");
        from.transferTo(to, amount);
        accountRepo.update(from);
        accountRepo.update(to);
        transactionRepo.save(new Transaction("TRANSFER_OUT", amount, fromId, toId));
        transactionRepo.save(new Transaction("TRANSFER_IN", amount, toId, fromId));
    }

    public Account getAccount(String accountId) {
        return accountRepo.findById(accountId);
    }
}
