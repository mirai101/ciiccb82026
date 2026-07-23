package com.oriosbank.api.service;

import com.oriosbank.api.dto.*;
import com.oriosbank.api.exception.InsufficientBalanceException;
import com.oriosbank.api.exception.InvalidAmountException;
import com.oriosbank.api.exception.ResourceNotFoundException;
import com.oriosbank.api.exception.UnauthorizedAccessException;
import com.oriosbank.api.model.*;
import com.oriosbank.api.repository.AccountRepository;
import com.oriosbank.api.repository.CardRepository;
import com.oriosbank.api.repository.CustomerRepository;
import com.oriosbank.api.repository.LoanRepository;
import com.oriosbank.api.repository.TransactionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    public static final double MAX_DEPOSIT = 100_000.0;
    public static final double MAX_WITHDRAWAL = 50_000.0;
    public static final double MAX_TRANSFER = 100_000.0;
    private static final int MAX_ACCOUNTS_PER_TYPE = 2;
    private static final int MAX_TRANSFERS_PER_DAY = 5;
    private static final int MAX_DEPOSITS_PER_DAY = 5;
    private static final int MAX_WITHDRAWALS_PER_DAY = 5;

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final LoanRepository loanRepository;

    public AccountService(AccountRepository accountRepository,
                          CustomerRepository customerRepository,
                          TransactionRepository transactionRepository,
                          CardRepository cardRepository,
                          LoanRepository loanRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.cardRepository = cardRepository;
        this.loanRepository = loanRepository;
    }

    @Transactional
    @CacheEvict(value = {"accounts", "balances"}, allEntries = true)
    public AccountDto openAccount(String customerId, String type, double initialDeposit) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Enforce max 2 accounts per type per user
        String typeUpper = type.toUpperCase();
        long existingCount = accountRepository.findByCustomerCustomerId(customerId).stream()
            .filter(a -> typeUpper.equals(a.getAccountType()))
            .count();
        if (existingCount >= MAX_ACCOUNTS_PER_TYPE) {
            throw new IllegalArgumentException("Maximum " + MAX_ACCOUNTS_PER_TYPE + " " + typeUpper + " accounts allowed per user");
        }

        // Enforce max balance limit
        double maxBalance = "SAVINGS".equalsIgnoreCase(type) ? SavingsAccount.MAX_BALANCE : CheckingAccount.MAX_BALANCE;
        if (initialDeposit > maxBalance) {
            throw new InvalidAmountException("Initial deposit cannot exceed $" + maxBalance);
        }

        Account account;
        String accountId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        if ("SAVINGS".equalsIgnoreCase(type)) {
            account = new SavingsAccount();
        } else if ("CHECKING".equalsIgnoreCase(type)) {
            account = new CheckingAccount();
        } else {
            throw new IllegalArgumentException("Invalid account type: " + type);
        }

        account.setAccountId(accountId);
        account.setCustomer(customer);
        account.setBalance(initialDeposit);
        account.setCreatedAt(LocalDateTime.now());

        accountRepository.save(account);

        if (initialDeposit > 0) {
            Transaction tx = Transaction.builder()
                .transactionId(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .type("INITIAL_DEPOSIT")
                .amount(initialDeposit)
                .toAccount(accountId)
                .account(account)
                .timestamp(LocalDateTime.now())
                .description("Initial deposit")
                .build();
            transactionRepository.save(tx);
        }

        return mapToDto(account);
    }

    @Cacheable(value = "accounts", key = "#customerId")
    @Transactional(readOnly = true)
    public List<AccountDto> getCustomerAccounts(String customerId) {
        return accountRepository.findByCustomerCustomerId(customerId)
            .stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Cacheable(value = "balances", key = "#customerId")
    @Transactional(readOnly = true)
    public double getTotalBalance(String customerId) {
        return accountRepository.getTotalBalanceByCustomerId(customerId);
    }

    @Transactional
    @CacheEvict(value = {"accounts", "balances", "transactions"}, allEntries = true)
    public void deposit(String customerId, DepositRequestDto dto) {
        // Check daily deposit frequency limit
        List<String> accountIds = accountRepository.findByCustomerCustomerId(customerId).stream()
            .map(Account::getAccountId).collect(Collectors.toList());
        long todayDeposits = transactionRepository.countByAccountIdsAndTypeAndTimestampAfter(
            accountIds, "DEPOSIT", LocalDateTime.now().toLocalDate().atStartOfDay());
        if (todayDeposits >= MAX_DEPOSITS_PER_DAY) {
            throw new InvalidAmountException("Daily deposit limit reached. Maximum " + MAX_DEPOSITS_PER_DAY + " deposits per day");
        }

        depositInternal(dto.getAccountId(), dto.getAmount(), dto.getDescription());
    }

    @Transactional
    @CacheEvict(value = {"accounts", "balances", "transactions"}, allEntries = true)
    public void depositInternal(String accountId, double amount, String description) {
        if (amount <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        if (amount > MAX_DEPOSIT) {
            throw new InvalidAmountException("Deposit limit exceeded. Maximum deposit per transaction is $" + MAX_DEPOSIT);
        }

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        double newBalance = account.getBalance() + amount;
        double maxBalance = (account instanceof SavingsAccount) ? SavingsAccount.MAX_BALANCE : CheckingAccount.MAX_BALANCE;
        if (newBalance > maxBalance) {
            throw new InvalidAmountException("Deposit would exceed maximum account balance of $" + maxBalance);
        }

        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
            .transactionId(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .type("DEPOSIT")
            .amount(amount)
            .account(account)
            .fromAccount(null)
            .toAccount(accountId)
            .timestamp(LocalDateTime.now())
            .description(description != null ? description : "Deposit to " + accountId)
            .build();
        transactionRepository.save(tx);
    }

    @Transactional
    @CacheEvict(value = {"accounts", "balances", "transactions"}, allEntries = true)
    public void withdraw(String customerId, WithdrawRequestDto dto) {
        if (dto.getAmount() <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        if (dto.getAmount() > MAX_WITHDRAWAL) {
            throw new InvalidAmountException("Withdrawal limit exceeded. Maximum withdrawal per transaction is $" + MAX_WITHDRAWAL);
        }

        // Check daily withdrawal frequency limit
        List<String> accountIds = accountRepository.findByCustomerCustomerId(customerId).stream()
            .map(Account::getAccountId).collect(Collectors.toList());
        long todayWithdrawals = transactionRepository.countByAccountIdsAndTypeAndTimestampAfter(
            accountIds, "WITHDRAW", LocalDateTime.now().toLocalDate().atStartOfDay());
        if (todayWithdrawals >= MAX_WITHDRAWALS_PER_DAY) {
            throw new InvalidAmountException("Daily withdrawal limit reached. Maximum " + MAX_WITHDRAWALS_PER_DAY + " withdrawals per day");
        }

        Account account = accountRepository.findById(dto.getAccountId())
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getCustomer().getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccessException("You do not own this account");
        }

        if (account instanceof SavingsAccount) {
            SavingsAccount savings = (SavingsAccount) account;
            if (account.getBalance() - dto.getAmount() < savings.getMinimumBalance()) {
                throw new InsufficientBalanceException("Minimum balance of $" + savings.getMinimumBalance() + " must be maintained");
            }
        } else if (account instanceof CheckingAccount) {
            CheckingAccount checking = (CheckingAccount) account;
            if (dto.getAmount() > account.getBalance() + checking.getOverdraftLimit()) {
                throw new InsufficientBalanceException("Overdraft limit of $" + checking.getOverdraftLimit() + " exceeded");
            }
        }

        account.setBalance(account.getBalance() - dto.getAmount());
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
            .transactionId(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .type("WITHDRAW")
            .amount(dto.getAmount())
            .account(account)
            .fromAccount(dto.getAccountId())
            .timestamp(LocalDateTime.now())
            .description(dto.getDescription() != null ? dto.getDescription() : "Withdrawal from " + dto.getAccountId())
            .build();
        transactionRepository.save(tx);
    }

    @Transactional
    public void withdrawInternal(String customerId, String accountId, double amount, String description) {
        if (amount <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getCustomer().getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccessException("You do not own this account");
        }

        if (account instanceof SavingsAccount) {
            SavingsAccount savings = (SavingsAccount) account;
            if (account.getBalance() - amount < savings.getMinimumBalance()) {
                throw new InsufficientBalanceException("Minimum balance of $" + savings.getMinimumBalance() + " must be maintained");
            }
        } else if (account instanceof CheckingAccount) {
            CheckingAccount checking = (CheckingAccount) account;
            if (amount > account.getBalance() + checking.getOverdraftLimit()) {
                throw new InsufficientBalanceException("Overdraft limit of $" + checking.getOverdraftLimit() + " exceeded");
            }
        }

        account.setBalance(account.getBalance() - amount);
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
            .transactionId(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .type("WITHDRAW")
            .amount(amount)
            .account(account)
            .fromAccount(accountId)
            .timestamp(LocalDateTime.now())
            .description(description != null ? description : "Withdrawal from " + accountId)
            .build();
        transactionRepository.save(tx);
    }

    @Transactional
    @CacheEvict(value = {"accounts", "balances", "transactions"}, allEntries = true)
    public void transfer(String customerId, TransferRequestDto dto) {
        if (dto.getAmount() <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        if (dto.getAmount() > MAX_TRANSFER) {
            throw new InvalidAmountException("Transfer limit exceeded. Maximum transfer per transaction is $" + MAX_TRANSFER);
        }

        // Check daily transfer frequency limit
        List<String> accountIds = accountRepository.findByCustomerCustomerId(customerId).stream()
            .map(Account::getAccountId).collect(Collectors.toList());
        long todayTransfers = transactionRepository.countByAccountIdsAndTypeAndTimestampAfter(
            accountIds, "TRANSFER_OUT", LocalDateTime.now().toLocalDate().atStartOfDay());
        if (todayTransfers >= MAX_TRANSFERS_PER_DAY) {
            throw new InvalidAmountException("Daily transfer limit reached. Maximum " + MAX_TRANSFERS_PER_DAY + " transfers per day");
        }

        Account from = accountRepository.findById(dto.getFromAccountId())
            .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));

        if (!from.getCustomer().getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccessException("You do not own the source account");
        }

        Account to = accountRepository.findById(dto.getToAccountId())
            .orElseThrow(() -> new ResourceNotFoundException("Target account not found"));

        if (from.getAccountId().equals(to.getAccountId())) {
            throw new InvalidAmountException("Cannot transfer to the same account");
        }

        if (from instanceof SavingsAccount) {
            SavingsAccount savings = (SavingsAccount) from;
            if (from.getBalance() - dto.getAmount() < savings.getMinimumBalance()) {
                throw new InsufficientBalanceException("Minimum balance of $" + savings.getMinimumBalance() + " must be maintained");
            }
        } else if (from instanceof CheckingAccount) {
            CheckingAccount checking = (CheckingAccount) from;
            if (dto.getAmount() > from.getBalance() + checking.getOverdraftLimit()) {
                throw new InsufficientBalanceException("Overdraft limit of $" + checking.getOverdraftLimit() + " exceeded");
            }
        }

        from.setBalance(from.getBalance() - dto.getAmount());
        to.setBalance(to.getBalance() + dto.getAmount());

        accountRepository.save(from);
        accountRepository.save(to);

        Transaction outTx = Transaction.builder()
            .transactionId(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .type("TRANSFER_OUT")
            .amount(dto.getAmount())
            .account(from)
            .fromAccount(dto.getFromAccountId())
            .toAccount(dto.getToAccountId())
            .timestamp(LocalDateTime.now())
            .description(dto.getDescription() != null ? dto.getDescription() : "Transfer to " + dto.getToAccountId())
            .build();

        Transaction inTx = Transaction.builder()
            .transactionId(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .type("TRANSFER_IN")
            .amount(dto.getAmount())
            .account(to)
            .fromAccount(dto.getFromAccountId())
            .toAccount(dto.getToAccountId())
            .timestamp(LocalDateTime.now())
            .description(dto.getDescription() != null ? dto.getDescription() : "Transfer from " + dto.getFromAccountId())
            .build();

        transactionRepository.save(outTx);
        transactionRepository.save(inTx);
    }

    private void checkAccountStatus(Account account) {
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new IllegalArgumentException("Account is " + account.getStatus());
        }
    }

    @Transactional
    public void toggleAccountVisibility(String customerId, String accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        if (!account.getCustomer().getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccessException("You don't own this account");
        }
        
        account.setHidden(!account.isHidden());
        accountRepository.save(account);
    }

    @Transactional
    public CardDto issueCard(String customerId, String accountId, String cardType) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        if (!account.getCustomer().getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccessException("You don't own this account");
        }
        
        Customer customer = account.getCustomer();
        
        String cardNumber = generateCardNumber();
        String cvv = String.format("%03d", (int)(Math.random() * 1000));
        String expiry = LocalDateTime.now().plusYears(5).format(java.time.format.DateTimeFormatter.ofPattern("MM/yy"));

        Card card = Card.builder()
            .cardId(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .cardNumber(cardNumber)
            .cardHolderName(customer.getFullName().toUpperCase())
            .expiryDate(expiry)
            .cvv(cvv)
            .cardType(cardType)
            .status("ACTIVE")
            .account(account)
            .customer(customer)
            .createdAt(LocalDateTime.now())
            .build();

        cardRepository.save(card);
        
        return mapToCardDto(card);
    }

    public List<CardDto> getMyCards(String customerId) {
        return cardRepository.findByCustomerCustomerId(customerId).stream()
            .map(this::mapToCardDto)
            .collect(Collectors.toList());
    }

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(String.format("%04d", (int)(Math.random() * 10000)));
            if (i < 3) sb.append("-");
        }
        return sb.toString();
    }

    private CardDto mapToCardDto(Card card) {
        return CardDto.builder()
            .cardId(card.getCardId())
            .cardNumber("****-****-****-" + card.getCardNumber().substring(card.getCardNumber().length() - 4))
            .cardHolderName(card.getCardHolderName())
            .expiryDate(card.getExpiryDate())
            .cardType(card.getCardType())
            .status(card.getStatus())
            .accountId(card.getAccount().getAccountId())
            .createdAt(card.getCreatedAt())
            .build();
    }

    // Admin Methods
    public List<CustomerDto> getAllCustomers(String adminId) {
        checkAdmin(adminId);
        return customerRepository.findAll().stream()
            .map(c -> CustomerDto.builder()
                .customerId(c.getCustomerId())
                .fullName(c.getFullName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .password(c.getHashedPassword()) // Show in hash mode
                .role(c.getRole())
                .registeredAt(c.getRegisteredAt())
                .build())
            .collect(Collectors.toList());
    }

    public List<AccountDto> getAllAccounts(String adminId) {
        checkAdmin(adminId);
        return accountRepository.findAll().stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public void updateAccountStatus(String adminId, String accountId, String status) {
        checkAdmin(adminId);
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        account.setStatus(status);
        accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public List<CardDto> getAllCards(String adminId) {
        checkAdmin(adminId);
        return cardRepository.findAll().stream()
                .map(this::mapToCardDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCard(String adminId, String cardId) {
        checkAdmin(adminId);
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        cardRepository.delete(card);
    }

    @Transactional
    public void deleteAccount(String adminId, String accountId) {
        checkAdmin(adminId);
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
        
        // Delete related transactions and cards
        // 1. Transactions linked by DBRef
        transactionRepository.deleteAll(transactionRepository.findByAccountAccountId(accountId));
        // 2. Transactions linked by account number strings (for transfers)
        transactionRepository.deleteAll(transactionRepository.findByFromAccountOrToAccount(accountId, accountId));
        
        cardRepository.deleteAll(cardRepository.findByAccountAccountId(accountId));
        
        accountRepository.delete(account);
    }

    @Transactional
    public void deleteCustomer(String adminId, String customerId) {
        checkAdmin(adminId);
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if ("ADMIN".equals(customer.getRole())) {
            throw new IllegalArgumentException("Cannot delete administrator account");
        }

        // Delete all accounts (which also deletes cards and transactions)
        List<Account> accounts = accountRepository.findByCustomerCustomerId(customerId);
        for (Account acc : accounts) {
            deleteAccount(adminId, acc.getAccountId());
        }

        // Final cleanup for any orphaned cards/transactions linked to customer but not accounts (shouldn't happen but for safety)
        cardRepository.deleteAll(cardRepository.findByCustomerCustomerId(customerId));
        loanRepository.deleteAll(loanRepository.findByCustomerId(customerId));

        customerRepository.delete(customer);
    }

    private void checkAdmin(String adminId) {
        Customer admin = customerRepository.findById(adminId)
            .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (!"ADMIN".equals(admin.getRole())) {
            throw new UnauthorizedAccessException("Requires Admin role");
        }
    }

    private AccountDto mapToDto(Account account) {
        return AccountDto.builder()
            .accountId(account.getAccountId())
            .customerId(account.getCustomer().getCustomerId())
            .type(account.getAccountType())
            .balance(account.getBalance())
            .interestRate(account.getInterestRate())
            .status(account.getStatus())
            .isHidden(account.isHidden())
            .createdAt(account.getCreatedAt())
            .build();
    }
}
