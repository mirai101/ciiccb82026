package com.oriosbank.api.controller;

import com.oriosbank.api.dto.*;
import com.oriosbank.api.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/open")
    public ResponseEntity<AccountDto> openAccount(
            @RequestParam("type") String type,
            @RequestParam("initialDeposit") double initialDeposit,
            Authentication auth) {
        String customerId = auth.getName();
        return ResponseEntity.ok(accountService.openAccount(customerId, type, initialDeposit));
    }

    @GetMapping("/my-accounts")
    public ResponseEntity<List<AccountDto>> getMyAccounts(Authentication auth) {
        String customerId = auth.getName();
        return ResponseEntity.ok(accountService.getCustomerAccounts(customerId));
    }

    @GetMapping("/total-balance")
    public ResponseEntity<Map<String, Double>> getTotalBalance(Authentication auth) {
        String customerId = auth.getName();
        return ResponseEntity.ok(Map.of("totalBalance", accountService.getTotalBalance(customerId)));
    }

    @PostMapping("/deposit")
    public ResponseEntity<Map<String, String>> deposit(@Valid @RequestBody DepositRequestDto dto) {
        accountService.deposit(dto);
        return ResponseEntity.ok(Map.of("message", "Deposit successful"));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Map<String, String>> withdraw(
            @Valid @RequestBody WithdrawRequestDto dto,
            Authentication auth) {
        String customerId = auth.getName();
        accountService.withdraw(customerId, dto);
        return ResponseEntity.ok(Map.of("message", "Withdrawal successful"));
    }

    @PostMapping("/transfer")
    public ResponseEntity<Map<String, String>> transfer(
            @Valid @RequestBody TransferRequestDto dto,
            Authentication auth) {
        String customerId = auth.getName();
        accountService.transfer(customerId, dto);
        return ResponseEntity.ok(Map.of("message", "Transfer successful"));
    }

    @PostMapping("/{accountId}/toggle-visibility")
    public ResponseEntity<Map<String, String>> toggleVisibility(
            @PathVariable String accountId,
            Authentication auth) {
        accountService.toggleAccountVisibility(auth.getName(), accountId);
        return ResponseEntity.ok(Map.of("message", "Visibility toggled"));
    }

    @PostMapping("/{accountId}/issue-card")
    public ResponseEntity<CardDto> issueCard(
            @PathVariable String accountId,
            @RequestParam String cardType,
            Authentication auth) {
        return ResponseEntity.ok(accountService.issueCard(auth.getName(), accountId, cardType));
    }

    @GetMapping("/my-cards")
    public ResponseEntity<List<CardDto>> getMyCards(Authentication auth) {
        return ResponseEntity.ok(accountService.getMyCards(auth.getName()));
    }

    // Admin Endpoints
    @GetMapping("/admin/all-customers")
    public ResponseEntity<List<CustomerDto>> getAllCustomers(Authentication auth) {
        return ResponseEntity.ok(accountService.getAllCustomers(auth.getName()));
    }

    @GetMapping("/admin/all-accounts")
    public ResponseEntity<List<AccountDto>> getAllAccounts(Authentication auth) {
        return ResponseEntity.ok(accountService.getAllAccounts(auth.getName()));
    }

    @GetMapping("/admin/all-cards")
    public ResponseEntity<List<CardDto>> getAllCards(Authentication auth) {
        return ResponseEntity.ok(accountService.getAllCards(auth.getName()));
    }

    @PostMapping("/admin/accounts/{accountId}/status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable String accountId,
            @RequestParam String status,
            Authentication auth) {
        accountService.updateAccountStatus(auth.getName(), accountId, status);
        return ResponseEntity.ok(Map.of("message", "Status updated to " + status));
    }

    @DeleteMapping("/admin/accounts/{accountId}")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @PathVariable String accountId,
            Authentication auth) {
        accountService.deleteAccount(auth.getName(), accountId);
        return ResponseEntity.ok(Map.of("message", "Account deleted"));
    }

    @DeleteMapping("/admin/cards/{cardId}")
    public ResponseEntity<Map<String, String>> deleteCard(
            @PathVariable String cardId,
            Authentication auth) {
        accountService.deleteCard(auth.getName(), cardId);
        return ResponseEntity.ok(Map.of("message", "Card deleted"));
    }

    @DeleteMapping("/admin/customers/{customerId}")
    public ResponseEntity<Map<String, String>> deleteCustomer(
            @PathVariable String customerId,
            Authentication auth) {
        accountService.deleteCustomer(auth.getName(), customerId);
        return ResponseEntity.ok(Map.of("message", "Customer and all associated data deleted"));
    }
}
