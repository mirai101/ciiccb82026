package com.oriosbank.api.service;

import com.oriosbank.api.dto.TransactionDto;
import com.oriosbank.api.exception.UnauthorizedAccessException;
import com.oriosbank.api.model.Customer;
import com.oriosbank.api.model.Transaction;
import com.oriosbank.api.repository.AccountRepository;
import com.oriosbank.api.repository.CustomerRepository;
import com.oriosbank.api.repository.TransactionRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              CustomerRepository customerRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    @Cacheable(value = "transactions", key = "#accountId")
    @Transactional(readOnly = true)
    public List<TransactionDto> getAccountTransactions(String accountId) {
        return transactionRepository.findByAccountAccountId(accountId)
            .stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getAllCustomerTransactions(String customerId) {
        return accountRepository.findByCustomerCustomerId(customerId)
            .stream()
            .flatMap(acc -> transactionRepository.findByAccountAccountId(acc.getAccountId()).stream())
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransactionDto> getAllTransactions(String adminId) {
        checkAdmin(adminId);
        return transactionRepository.findAll()
            .stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    private void checkAdmin(String adminId) {
        Customer admin = customerRepository.findById(adminId)
            .orElseThrow(() -> new UnauthorizedAccessException("Admin not found"));
        if (!"ADMIN".equals(admin.getRole())) {
            throw new UnauthorizedAccessException("Requires Admin role");
        }
    }

    private TransactionDto mapToDto(Transaction tx) {
        return TransactionDto.builder()
            .transactionId(tx.getTransactionId())
            .type(tx.getType())
            .amount(tx.getAmount())
            .timestamp(tx.getTimestamp())
            .fromAccount(tx.getFromAccount())
            .toAccount(tx.getToAccount())
            .accountId(tx.getAccount() != null ? tx.getAccount().getAccountId() : null)
            .description(tx.getDescription())
            .build();
    }
}
