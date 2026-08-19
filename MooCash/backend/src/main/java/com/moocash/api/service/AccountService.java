package com.moocash.api.service;

import com.moocash.api.dto.*;
import com.moocash.api.exception.InsufficientBalanceException;
import com.moocash.api.exception.InvalidAmountException;
import com.moocash.api.exception.ResourceNotFoundException;
import com.moocash.api.exception.UnauthorizedAccessException;
import com.moocash.api.model.*;
import com.moocash.api.repository.AccountRepository;
import com.moocash.api.repository.CardRepository;
import com.moocash.api.repository.CustomerRepository;
import com.moocash.api.repository.LoanRepository;
import com.moocash.api.repository.TransactionRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    public static final BigDecimal MAX_DEPOSIT = new BigDecimal("100000.00");
    public static final BigDecimal MAX_WITHDRAWAL = new BigDecimal("50000.00");
    public static final BigDecimal MAX_TRANSFER = new BigDecimal("100000.00");
    private static final int MAX_ACCOUNTS_PER_TYPE = 2;
    private static final int MAX_TRANSFERS_PER_DAY = 5;
    private static final int MAX_DEPOSITS_PER_DAY = 5;
    private static final int MAX_WITHDRAWALS_PER_DAY = 5;

    // java.util.Random / Math.random() are not cryptographically secure and are
    // predictable - never use them for anything resembling card credentials.
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
    public AccountDto openAccount(String customerId, String type, BigDecimal initialDeposit) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        BigDecimal deposit = normalize(initialDeposit != null ? initialDeposit : BigDecimal.ZERO);
        if (deposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidAmountException("Initial deposit cannot be negative");
        }

        String typeUpper = type.toUpperCase();
        long existingCount = accountRepository.findByCustomerCustomerId(customerId).stream()
            .filter(a -> typeUpper.equals(a.getAccountType()))
            .count();
        if (existingCount >= MAX_ACCOUNTS_PER_TYPE) {
            throw new IllegalArgumentException("Maximum " + MAX_ACCOUNTS_PER_TYPE + " " + typeUpper + " accounts allowed per user");
        }

        Account account;
        if ("SAVINGS".equals(typeUpper)) {
            account = new SavingsAccount();
        } else if ("CHECKING".equals(typeUpper)) {
            account = new CheckingAccount();
        } else {
            throw new IllegalArgumentException("Invalid account type: " + type);
        }

        BigDecimal maxBalance = "SAVINGS".equals(typeUpper) ? SavingsAccount.MAX_BALANCE : CheckingAccount.MAX_BALANCE;
        if (deposit.compareTo(maxBalance) > 0) {
            throw new InvalidAmountException("Initial deposit cannot exceed $" + maxBalance);
        }

        String accountId = newId();
        account.setAccountId(accountId);
        account.setCustomer(customer);
        account.setBalance(deposit);
        account.setCreatedAt(LocalDateTime.now());

        accountRepository.save(account);

        if (deposit.compareTo(BigDecimal.ZERO) > 0) {
            Transaction tx = Transaction.builder()
                .transactionId(newId())
                .type("INITIAL_DEPOSIT")
                .amount(deposit)
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
    public BigDecimal getTotalBalance(String customerId) {
        return normalize(accountRepository.getTotalBalanceByCustomerId(customerId));
    }

    @Transactional
    @CacheEvict(value = {"accounts", "balances", "transactions"}, allEntries = true)
    public void deposit(String customerId, DepositRequestDto dto) {
        List<String> accountIds = accountRepository.findByCustomerCustomerId(customerId).stream()
            .map(Account::getAccountId).collect(Collectors.toList());
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long todayDeposits = transactionRepository
            .countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(accountIds, "DEPOSIT", startOfDay);
        if (todayDeposits >= MAX_DEPOSITS_PER_DAY) {
            throw new InvalidAmountException("Daily deposit limit reached. Maximum " + MAX_DEPOSITS_PER_DAY + " deposits per day");
        }

        depositInternal(dto.getAccountId(), dto.getAmount(), dto.getDescription());
    }

    @Transactional
    @CacheEvict(value = {"accounts", "balances", "transactions"}, allEntries = true)
    public void depositInternal(String accountId, BigDecimal rawAmount, String description) {
        BigDecimal amount = normalize(rawAmount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        if (amount.compareTo(MAX_DEPOSIT) > 0) {
            throw new InvalidAmountException("Deposit limit exceeded. Maximum deposit per transaction is $" + MAX_DEPOSIT);
        }

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        BigDecimal newBalance = account.getBalance().add(amount);
        BigDecimal maxBalance = (account instanceof SavingsAccount) ? SavingsAccount.MAX_BALANCE : CheckingAccount.MAX_BALANCE;
        if (newBalance.compareTo(maxBalance) > 0) {
            throw new InvalidAmountException("Deposit would exceed maximum account balance of $" + maxBalance);
        }

        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
            .transactionId(newId())
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
        BigDecimal amount = normalize(dto.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        if (amount.compareTo(MAX_WITHDRAWAL) > 0) {
            throw new InvalidAmountException("Withdrawal limit exceeded. Maximum withdrawal per transaction is $" + MAX_WITHDRAWAL);
        }

        List<String> accountIds = accountRepository.findByCustomerCustomerId(customerId).stream()
            .map(Account::getAccountId).collect(Collectors.toList());
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long todayWithdrawals = transactionRepository
            .countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(accountIds, "WITHDRAW", startOfDay);
        if (todayWithdrawals >= MAX_WITHDRAWALS_PER_DAY) {
            throw new InvalidAmountException("Daily withdrawal limit reached. Maximum " + MAX_WITHDRAWALS_PER_DAY + " withdrawals per day");
        }

        Account account = accountRepository.findById(dto.getAccountId())
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getCustomer().getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccessException("You do not own this account");
        }

        assertWithdrawalAllowed(account, amount);

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
            .transactionId(newId())
            .type("WITHDRAW")
            .amount(amount)
            .account(account)
            .fromAccount(dto.getAccountId())
            .timestamp(LocalDateTime.now())
            .description(dto.getDescription() != null ? dto.getDescription() : "Withdrawal from " + dto.getAccountId())
            .build();
        transactionRepository.save(tx);
    }

    /**
     * Internal withdrawal used by loan repayment / auto-debt. Same rules as the
     * public withdraw() but without the daily-limit check, since these are
     * system-initiated, not customer-initiated, transactions.
     */
    @Transactional
    public void withdrawInternal(String customerId, String accountId, BigDecimal rawAmount, String description) {
        BigDecimal amount = normalize(rawAmount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (!account.getCustomer().getCustomerId().equals(customerId)) {
            throw new UnauthorizedAccessException("You do not own this account");
        }

        assertWithdrawalAllowed(account, amount);

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
            .transactionId(newId())
            .type("WITHDRAW")
            .amount(amount)
            .account(account)
            .fromAccount(accountId)
            .timestamp(LocalDateTime.now())
            .description(description != null ? description : "Withdrawal from " + accountId)
            .build();
        transactionRepository.save(tx);
    }

    private void assertWithdrawalAllowed(Account account, BigDecimal amount) {
        if (account instanceof SavingsAccount savings) {
            BigDecimal remaining = account.getBalance().subtract(amount);
            if (remaining.compareTo(savings.getMinimumBalance()) < 0) {
                throw new InsufficientBalanceException("Minimum balance of $" + savings.getMinimumBalance() + " must be maintained");
            }
        } else if (account instanceof CheckingAccount checking) {
            BigDecimal available = account.getBalance().add(checking.getOverdraftLimit());
            if (amount.compareTo(available) > 0) {
                throw new InsufficientBalanceException("Overdraft limit of $" + checking.getOverdraftLimit() + " exceeded");
            }
        }
    }

    @Transactional
    @CacheEvict(value = {"accounts", "balances", "transactions"}, allEntries = true)
    public void transfer(String customerId, TransferRequestDto dto) {
        BigDecimal amount = normalize(dto.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive");
        }
        if (amount.compareTo(MAX_TRANSFER) > 0) {
            throw new InvalidAmountException("Transfer limit exceeded. Maximum transfer per transaction is $" + MAX_TRANSFER);
        }

        List<String> accountIds = accountRepository.findByCustomerCustomerId(customerId).stream()
            .map(Account::getAccountId).collect(Collectors.toList());
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        long todayTransfers = transactionRepository
            .countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(accountIds, "TRANSFER_OUT", startOfDay);
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

        assertWithdrawalAllowed(from, amount);

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        accountRepository.save(from);
        accountRepository.save(to);

        Transaction outTx = Transaction.builder()
            .transactionId(newId())
            .type("TRANSFER_OUT")
            .amount(amount)
            .account(from)
            .fromAccount(dto.getFromAccountId())
            .toAccount(dto.getToAccountId())
            .timestamp(LocalDateTime.now())
            .description(dto.getDescription() != null ? dto.getDescription() : "Transfer to " + dto.getToAccountId())
            .build();

        Transaction inTx = Transaction.builder()
            .transactionId(newId())
            .type("TRANSFER_IN")
            .amount(amount)
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
        String cvv = generateCvv();
        String expiry = LocalDateTime.now().plusYears(5).format(java.time.format.DateTimeFormatter.ofPattern("MM/yy"));

        // Bug fix: the original code called .cardHolderName(...) twice on the
        // builder (first name, then last name), so the second call silently
        // overwrote the first and the customer's first name was lost. Combine
        // both into a single value instead.
        String fullName = (customer.getFirstName() + " " + customer.getLastName()).trim().toUpperCase();

        Card card = Card.builder()
            .cardId(newId())
            .cardNumber(cardNumber)
            .cardHolderName(fullName)
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

    @Transactional(readOnly = true)
    public List<CardDto> getMyCards(String customerId) {
        return cardRepository.findByCustomerCustomerId(customerId).stream()
            .map(this::mapToCardDto)
            .collect(Collectors.toList());
    }

    private String generateCardNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(String.format("%04d", SECURE_RANDOM.nextInt(10000)));
            if (i < 3) sb.append("-");
        }
        return sb.toString();
    }

    private String generateCvv() {
        return String.format("%03d", SECURE_RANDOM.nextInt(1000));
    }

    private CardDto mapToCardDto(Card card) {
        return CardDto.builder()
            .cardId(card.getCardId())
            .cardNumber(" " + card.getCardNumber().substring(card.getCardNumber().length() - 4))
            .cardHolderName(card.getCardHolderName())
            .expiryDate(card.getExpiryDate())
            .cardType(card.getCardType())
            .status(card.getStatus())
            .accountId(card.getAccount().getAccountId())
            .createdAt(card.getCreatedAt())
            .build();
    }

    public List<CustomerDto> getAllCustomers(String adminId) {
        checkAdmin(adminId);
        return customerRepository.findAll().stream()
            .map(c -> CustomerDto.builder()
                .customerId(c.getCustomerId())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .email(c.getEmail())
                .phone(c.getPhone())
                // NOTE: this intentionally omits the password hash from the response
                // (see CustomerDto) - the previous version exposed hashedPassword
                // to the admin frontend unnecessarily. Password recovery should go
                // through adminChangePassword() instead of displaying hashes.
                .role(c.getRole())
                .registeredAt(c.getRegisteredAt())
                .build())
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
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

        transactionRepository.deleteAll(transactionRepository.findByAccountAccountId(accountId));
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

        List<Account> accounts = accountRepository.findByCustomerCustomerId(customerId);
        for (Account acc : accounts) {
            deleteAccount(adminId, acc.getAccountId());
        }

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

    private static String newId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
