package com.moocash.api.controller;

import com.moocash.api.dto.LoanDto;
import com.moocash.api.dto.LoanRequestDto;
import com.moocash.api.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PostMapping("/request")
    public ResponseEntity<LoanDto> requestLoan(
            @Valid @RequestBody LoanRequestDto dto,
            Authentication auth) {
        return ResponseEntity.ok(loanService.requestLoan(auth.getName(), dto));
    }

    @GetMapping("/my-loans")
    public ResponseEntity<List<LoanDto>> getMyLoans(Authentication auth) {
        return ResponseEntity.ok(loanService.getMyLoans(auth.getName()));
    }

    @PostMapping("/repay")
    public ResponseEntity<Map<String, String>> repayLoan(
            @RequestParam String loanId,
            @RequestParam String fromAccountId,
            @RequestParam BigDecimal amount,
            Authentication auth) {
        loanService.repayLoan(loanId, fromAccountId, auth.getName(), amount);
        return ResponseEntity.ok(Map.of("message", "Repayment successful"));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<LoanDto>> getAllLoans(Authentication auth) {
        return ResponseEntity.ok(loanService.getAllLoans(auth.getName()));
    }

    @PostMapping("/admin/{loanId}/approve")
    public ResponseEntity<Map<String, String>> approveLoan(
            @PathVariable String loanId,
            @RequestParam String targetAccountId,
            Authentication auth) {
        loanService.approveLoan(auth.getName(), loanId, targetAccountId);
        return ResponseEntity.ok(Map.of("message", "Loan approved and funds disbursed"));
    }

    @PostMapping("/admin/{loanId}/reject")
    public ResponseEntity<Map<String, String>> rejectLoan(
            @PathVariable String loanId,
            Authentication auth) {
        loanService.rejectLoan(auth.getName(), loanId);
        return ResponseEntity.ok(Map.of("message", "Loan rejected"));
    }

    @PostMapping("/admin/{loanId}/auto-debt")
    public ResponseEntity<Map<String, String>> toggleAutoDebt(
            @PathVariable String loanId,
            @RequestParam boolean enabled,
            Authentication auth) {
        loanService.toggleAutoDebt(auth.getName(), loanId, enabled);
        return ResponseEntity.ok(Map.of("message", "Auto-debt " + (enabled ? "enabled" : "disabled")));
    }

    @PostMapping("/admin/process-auto-debts")
    public ResponseEntity<Map<String, String>> processAutoDebts(Authentication auth) {
        loanService.processAutoDebtsManual(auth.getName());
        return ResponseEntity.ok(Map.of("message", "Auto-debt processing triggered"));
    }
}
