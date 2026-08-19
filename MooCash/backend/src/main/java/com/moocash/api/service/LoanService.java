package com.moocash.api.service;

import com.moocash.api.dto.LoanDto;
import com.moocash.api.dto.LoanRequestDto;
import com.moocash.api.exception.ResourceNotFoundException;
import com.moocash.api.exception.UnauthorizedAccessException;
import com.moocash.api.model.Account;
import com.moocash.api.model.Customer;
import com.moocash.api.model.Loan;
import com.moocash.api.repository.AccountRepository;
import com.moocash.api.repository.CustomerRepository;
import com.moocash.api.repository.LoanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LoanService {

    public static final BigDecimal MAX_LOAN_AMOUNT = new BigDecimal("2000000.00");
    private static final BigDecimal AUTO_DEBT_RATE = new BigDecimal("0.05");
    private static final BigDecimal ZERO_THRESHOLD = new BigDecimal("0.01");
    private static final int MAX_LOANS_PER_WEEK = 2;

    private final LoanRepository loanRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    public LoanService(LoanRepository loanRepository,
                       CustomerRepository customerRepository,
                       AccountRepository accountRepository,
                       AccountService accountService) {
        this.loanRepository = loanRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
    }

    public LoanDto requestLoan(String customerId, LoanRequestDto dto) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (dto.getAmount().compareTo(MAX_LOAN_AMOUNT) > 0) {
            throw new IllegalArgumentException("Loan amount cannot exceed $" + MAX_LOAN_AMOUNT);
        }

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<Loan> allLoans = loanRepository.findByCustomerId(customerId);
        long recentLoans = allLoans.stream()
            .filter(l -> l.getCreatedAt() != null && !l.getCreatedAt().isBefore(sevenDaysAgo))
            .count();
        if (recentLoans >= MAX_LOANS_PER_WEEK) {
            throw new IllegalArgumentException("Loan request limit reached. Maximum " + MAX_LOANS_PER_WEEK + " loan requests per 7 days");
        }

        BigDecimal amount = dto.getAmount().setScale(2, RoundingMode.HALF_UP);

        Loan loan = Loan.builder()
                .loanId(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customer)
                .amount(amount)
                .remainingBalance(amount)
                .interestRate(dto.getInterestRate())
                .status("PENDING")
                .autoDebtEnabled(false)
                .createdAt(LocalDateTime.now())
                .build();

        return mapToDto(loanRepository.save(loan));
    }

    @Transactional(readOnly = true)
    public List<LoanDto> getMyLoans(String customerId) {
        return loanRepository.findByCustomerId(customerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LoanDto> getAllLoans(String adminId) {
        verifyAdmin(adminId);

        return loanRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void approveLoan(String adminId, String loanId, String targetAccountId) {
        verifyAdmin(adminId);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (!"PENDING".equals(loan.getStatus())) {
            throw new IllegalArgumentException("Loan is not in PENDING status");
        }

        accountRepository.findById(targetAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not Found: " + targetAccountId));

        loan.setStatus("APPROVED");
        loanRepository.save(loan);

        accountService.depositInternal(targetAccountId, loan.getAmount(), "Loan Approval: " + loanId);
    }

    public void rejectLoan(String adminId, String loanId) {
        verifyAdmin(adminId);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (!"PENDING".equals(loan.getStatus())) {
            throw new IllegalArgumentException("Loan is not in PENDING status");
        }

        loan.setStatus("REJECTED");
        loanRepository.save(loan);
    }

    @Transactional
    public void toggleAutoDebt(String adminId, String loanId, boolean enabled) {
        verifyAdmin(adminId);
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        loan.setAutoDebtEnabled(enabled);
        loanRepository.save(loan);
    }

    @Transactional
    public void processAutoDebtsManual(String adminId) {
        verifyAdmin(adminId);
        processAutoDebts();
    }

    @Transactional
    public void repayLoan(String loanId, String fromAccountId, String customerId, BigDecimal amount) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not Found: " + fromAccountId));

        repayInternal(loan, fromAccountId, amount);
    }

    @Transactional
    public void processAutoDebts() {
        List<Loan> autoDebtLoans = loanRepository.findAll().stream()
                .filter(l -> l.isAutoDebtEnabled() && "APPROVED".equals(l.getStatus()))
                .toList();

        for (Loan loan : autoDebtLoans) {
            try {
                BigDecimal amountToRepay = loan.getAmount().multiply(AUTO_DEBT_RATE).setScale(2, RoundingMode.HALF_UP);
                if (amountToRepay.compareTo(loan.getRemainingBalance()) > 0) {
                    amountToRepay = loan.getRemainingBalance();
                }

                final BigDecimal finalRepaymentAmount = amountToRepay;

                List<Account> accounts = accountRepository.findByCustomerCustomerId(loan.getCustomer().getCustomerId());
                accounts.stream()
                        .filter(a -> "ACTIVE".equals(a.getStatus()) && a.getBalance().compareTo(finalRepaymentAmount) >= 0)
                        .findFirst().ifPresent(sourceAccount -> repayInternal(loan, sourceAccount.getAccountId(), finalRepaymentAmount));

            } catch (Exception e) {
                log.error("Failed to process auto-debt for loan {}: {}", loan.getLoanId(), e.getMessage(), e);
            }
        }
    }

    private void repayInternal(Loan loan, String fromAccountId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return;

        if (amount.compareTo(loan.getRemainingBalance()) > 0) {
            amount = loan.getRemainingBalance();
        }

        accountService.withdrawInternal(loan.getCustomer().getCustomerId(), fromAccountId, amount, "Loan Repayment: " + loan.getLoanId());

        loan.setRemainingBalance(loan.getRemainingBalance().subtract(amount));
        if (loan.getRemainingBalance().compareTo(ZERO_THRESHOLD) <= 0) {
            loan.setStatus("PAID");
            loan.setPaidAt(LocalDateTime.now());
        }
        loanRepository.save(loan);
    }

    private void verifyAdmin(String adminId) {
        Customer admin = customerRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (!"ADMIN".equals(admin.getRole())) {
            throw new UnauthorizedAccessException("Access denied: Admin role required");
        }
    }

    private LoanDto mapToDto(Loan loan) {
        return LoanDto.builder()
                .loanId(loan.getLoanId())
                .customerId(loan.getCustomer().getCustomerId())
                .customerName(
                        loan.getCustomer().getFirstName() + " " +
                                loan.getCustomer().getLastName()
                )
                .amount(loan.getAmount())
                .remainingBalance(loan.getRemainingBalance())
                .interestRate(loan.getInterestRate())
                .status(loan.getStatus())
                .autoDebtEnabled(loan.isAutoDebtEnabled())
                .createdAt(loan.getCreatedAt())
                .build();
    }
}
