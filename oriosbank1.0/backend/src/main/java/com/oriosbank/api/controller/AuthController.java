package com.oriosbank.api.controller;

import com.oriosbank.api.dto.*;
import com.oriosbank.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody CustomerDto dto) {
        return ResponseEntity.ok(authService.register(dto));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerDto> getMyDetails(Authentication auth) {
        String customerId = auth.getName();
        return ResponseEntity.ok(authService.getCustomer(customerId));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody PasswordChangeDto dto,
            Authentication auth) {
        authService.changePassword(auth.getName(), dto);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/admin/change-password/{customerId}")
    public ResponseEntity<Map<String, String>> adminChangePassword(
            @PathVariable String customerId,
            @RequestBody Map<String, String> payload,
            Authentication auth) {
        String newPassword = payload.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid new password"));
        }
        authService.adminChangePassword(auth.getName(), customerId, newPassword);
        return ResponseEntity.ok(Map.of("message", "User password updated successfully"));
    }
}
