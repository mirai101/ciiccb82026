package com.oriosbank.api.service;

import com.oriosbank.api.dto.LoanDto;
import com.oriosbank.api.dto.LoanRequestDto;
import com.oriosbank.api.exception.ResourceNotFoundException;
import com.oriosbank.api.exception.UnauthorizedAccessException;
import com.oriosbank.api.model.Account;
import com.oriosbank.api.model.Customer;
import com.oriosbank.api.model.Loan;
import com.oriosbank.api.repository.AccountRepository;
import com.oriosbank.api.repository.CustomerRepository;
import com.oriosbank.api.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LoanService {

    public static final double MAX_LOAN_AMOUNT = 2_000_000.0;

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

        if (dto.getAmount() > MAX_LOAN_AMOUNT) {
            throw new IllegalArgumentException("Loan amount cannot exceed $" + MAX_LOAN_AMOUNT);
        }

        Loan loan = Loan.builder()
                .loanId(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customer)
                .amount(dto.getAmount())
                .remainingBalance(dto.getAmount())
                .interestRate(dto.getInterestRate())
                .status("PENDING")
                .autoDebtEnabled(false)
                .createdAt(LocalDateTime.now())
                .build();

        return mapToDto(loanRepository.save(loan));
    }

    public List<LoanDto> getMyLoans(String customerId) {
        return loanRepository.findByCustomerId(customerId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

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
    public void repayLoan(String loanId, String fromAccountId, String customerId, double amount) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (amount <= 0) {
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
                // Determine repayment amount (e.g., 5% of original amount or remaining balance)
                double amountToRepay = loan.getAmount() * 0.05;
                if (amountToRepay > loan.getRemainingBalance()) {
                    amountToRepay = loan.getRemainingBalance();
                }

                final double finalRepaymentAmount = amountToRepay;

                // Find an active account with enough balance
                List<Account> accounts = accountRepository.findByCustomerCustomerId(loan.getCustomer().getCustomerId());
                accounts.stream()
                        .filter(a -> "ACTIVE".equals(a.getStatus()) && a.getBalance() >= finalRepaymentAmount)
                        .findFirst().ifPresent(sourceAccount -> repayInternal(loan, sourceAccount.getAccountId(), finalRepaymentAmount));

            } catch (Exception e) {
                // Log and continue with next loan
                System.err.println("Failed to process auto-debt for loan " + loan.getLoanId() + ": " + e.getMessage());
            }
        }
    }

    private void repayInternal(Loan loan, String fromAccountId, double amount) {
        if (amount <= 0) return;

        if (amount > loan.getRemainingBalance()) {
            amount = loan.getRemainingBalance();
        }

        accountService.withdrawInternal(loan.getCustomer().getCustomerId(), fromAccountId, amount, "Loan Repayment: " + loan.getLoanId());

        loan.setRemainingBalance(loan.getRemainingBalance() - amount);
        if (loan.getRemainingBalance() <= 0.001) {
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
                .customerName(loan.getCustomer().getFullName())
                .amount(loan.getAmount())
                .remainingBalance(loan.getRemainingBalance())
                .interestRate(loan.getInterestRate())
                .status(loan.getStatus())
                .autoDebtEnabled(loan.isAutoDebtEnabled())
                .createdAt(loan.getCreatedAt())
                .build();
    }
}
