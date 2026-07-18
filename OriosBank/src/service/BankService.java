package service;

import model.Account;
import model.Customer;
import model.SavingsAccount;
import model.CheckingAccount;
import repository.AccountRepository;
import repository.CustomerRepository;
import repository.TransactionRepository;
import model.Transaction;

import java.util.List;

public class BankService {
    private final CustomerRepository customerRepo;
    private final AccountRepository accountRepo;
    private final TransactionRepository transactionRepo;

    public BankService() {
        this.customerRepo = new CustomerRepository();
        this.accountRepo = new AccountRepository();
        this.transactionRepo = new TransactionRepository();
    }

    public Customer registerCustomer(String fullName, String email, String password, String phone) {
        if (customerRepo.findByEmail(email) != null) {
            System.out.println("Email already registered.");
            return null;
        }
        Customer customer = new Customer(fullName, email, password, phone);
        customerRepo.save(customer);
        return customer;
    }

    public Customer login(String email, String password) {
        Customer customer = customerRepo.findByEmail(email);
        if (customer != null && customer.verifyPassword(password)) {
            return customer;
        }
        return null;
    }

    public Account openAccount(String customerId, String type, double initialDeposit) {
        Account account;
        if (type.equalsIgnoreCase("SAVINGS")) {
            account = new SavingsAccount(customerId, initialDeposit);
        } else {
            account = new CheckingAccount(customerId, initialDeposit);
        }
        accountRepo.save(account);
        if (initialDeposit > 0) {
            transactionRepo.save(new Transaction("INITIAL_DEPOSIT", initialDeposit, null, account.getAccountId()));
        }
        return account;
    }

    public List<Account> getCustomerAccounts(String customerId) {
        return accountRepo.findByCustomerId(customerId);
    }

    public double getTotalBalance(String customerId) {
        return getCustomerAccounts(customerId).stream().mapToDouble(Account::getBalance).sum();
    }
}
