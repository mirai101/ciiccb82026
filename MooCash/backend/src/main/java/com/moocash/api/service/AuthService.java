package com.moocash.api.service;

import com.moocash.api.dto.*;
import com.moocash.api.exception.ResourceNotFoundException;
import com.moocash.api.exception.UnauthorizedAccessException;
import com.moocash.api.model.Customer;
import com.moocash.api.repository.CustomerRepository;
import com.moocash.api.security.JwtUtil;
import com.moocash.api.security.PasswordHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
public class AuthService {

    private final CustomerRepository customerRepository;
    private final PasswordHasher passwordHasher;
    private final JwtUtil jwtUtil;

    public AuthService(CustomerRepository customerRepository, 
                       PasswordHasher passwordHasher, 
                       JwtUtil jwtUtil) {
        this.customerRepository = customerRepository;
        this.passwordHasher = passwordHasher;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public AuthResponseDto register(CustomerDto dto) {
        if (customerRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        Customer customer = Customer.builder()
            .customerId(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .firstName(dto.getFirstName())
            .lastName(dto.getLastName())
            .email(dto.getEmail())
            .hashedPassword(passwordHasher.hash(dto.getPassword()))
            .phone(dto.getPhone())
            .tokenVersion(0)
            .build();

        customerRepository.save(customer);

        String role = customer.getRole();
        String token = jwtUtil.generateToken(customer.getCustomerId(), customer.getEmail(), role, customer.getTokenVersion());

        return AuthResponseDto.builder()
            .token(token)
            .type("Bearer")
            .customerId(customer.getCustomerId())
            .fullName(fullName(customer))
            .email(customer.getEmail())
            .role(role)
            .expiresIn(jwtUtil.getExpirationTime() / 1000)
            .build();
    }

    @Transactional
    public AuthResponseDto login(LoginRequestDto dto) {
        Customer customer = customerRepository.findByEmail(dto.getEmail())
            .orElseThrow(() -> new UnauthorizedAccessException("Invalid credentials"));

        if (!passwordHasher.verify(dto.getPassword(), customer.getHashedPassword())) {
            throw new UnauthorizedAccessException("Invalid credentials");
        }

        customer.setTokenVersion(customer.getTokenVersion() + 1);
        customerRepository.save(customer);

        String role = customer.getRole();
        String token = jwtUtil.generateToken(customer.getCustomerId(), customer.getEmail(), role, customer.getTokenVersion());

        return AuthResponseDto.builder()
                .token(token)
                .type("Bearer")
                .customerId(customer.getCustomerId())
                .fullName(fullName(customer))
                .email(customer.getEmail())
                .role(role)
                .expiresIn(jwtUtil.getExpirationTime() / 1000)
                .build();
    }

    // Bug fix: both register() and login() used to call .fullName(...) twice on
    // the AuthResponseDto builder (once with firstName, once with lastName) -
    // the second call silently overwrote the first, so only the last name ever
    // reached the client. Centralizing the combination here avoids repeating
    // the mistake.
    private String fullName(Customer customer) {
        String first = customer.getFirstName() != null ? customer.getFirstName() : "";
        String last = customer.getLastName() != null ? customer.getLastName() : "";
        return (first + " " + last).trim();
    }

    @Cacheable(value = "customers", key = "#customerId")
    @Transactional(readOnly = true)
    public CustomerDto getCustomer(String customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        return CustomerDto.builder()
            .customerId(customer.getCustomerId())
            .firstName(customer.getFirstName())
            .lastName(customer.getLastName())
            .email(customer.getEmail())
            .phone(customer.getPhone())
            .role(customer.getRole())
            .registeredAt(customer.getRegisteredAt())
            .build();
    }

    @Transactional
    public void changePassword(String customerId, PasswordChangeDto dto) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        if (!passwordHasher.verify(dto.getCurrentPassword(), customer.getHashedPassword())) {
            throw new UnauthorizedAccessException("Current password incorrect");
        }

        customer.setHashedPassword(passwordHasher.hash(dto.getNewPassword()));
        customerRepository.save(customer);
    }

    @Transactional
    public void adminChangePassword(String adminId, String targetCustomerId, String newPassword) {
        Customer admin = customerRepository.findById(adminId)
            .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        
        if (!"ADMIN".equals(admin.getRole())) {
            throw new UnauthorizedAccessException("Requires Admin role");
        }

        Customer target = customerRepository.findById(targetCustomerId)
            .orElseThrow(() -> new ResourceNotFoundException("Target customer not found"));

        target.setHashedPassword(passwordHasher.hash(newPassword));
        customerRepository.save(target);
    }

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Transactional
    public void setupAdmin() {

        if (customerRepository.existsById("ADMIN001")) {
            return;
        }

        if (customerRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }

        Customer admin = Customer.builder()
                .customerId("ADMIN001")
                .firstName("System Administrator")
                .email(adminEmail)
                .hashedPassword(passwordHasher.hash(adminPassword))
                .phone("0000000000")
                .role("ADMIN")
                .tokenVersion(0)
                .build();

        customerRepository.save(admin);
    }
}
