package com.oriosbank.api.controller;

import com.oriosbank.api.dto.TransactionDto;
import com.oriosbank.api.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<TransactionDto>> getAccountTransactions(@PathVariable String accountId) {
        return ResponseEntity.ok(transactionService.getAccountTransactions(accountId));
    }

    @GetMapping("/my-transactions")
    public ResponseEntity<List<TransactionDto>> getMyTransactions(Authentication auth) {
        String customerId = auth.getName();
        return ResponseEntity.ok(transactionService.getAllCustomerTransactions(customerId));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<TransactionDto>> getAllTransactions(Authentication auth) {
        return ResponseEntity.ok(transactionService.getAllTransactions(auth.getName()));
    }
}
