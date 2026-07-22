package com.oriosbank.api.service;

import com.oriosbank.api.dto.*;
import com.oriosbank.api.exception.ResourceNotFoundException;
import com.oriosbank.api.exception.UnauthorizedAccessException;
import com.oriosbank.api.model.Customer;
import com.oriosbank.api.repository.CustomerRepository;
import com.oriosbank.api.security.JwtUtil;
import com.oriosbank.api.security.PasswordHasher;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service responsible for authentication and user management.
 * Handles registration, login, password changes, and admin setup.
 */
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

    /**
     * Registers a new customer in the system.
     * @param dto the customer data transfer object
     * @return AuthResponseDto containing the JWT token and user info
     * @throws IllegalArgumentException if the email is already registered
     */
    @Transactional
    public AuthResponseDto register(CustomerDto dto) {
        if (customerRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        Customer customer = Customer.builder()
            .customerId(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            .fullName(dto.getFullName())
            .email(dto.getEmail())
            .hashedPassword(passwordHasher.hash(dto.getPassword()))
            .phone(dto.getPhone())
            .build();

        customerRepository.save(customer);
        
        String role = customer.getRole();
        String token = jwtUtil.generateToken(customer.getCustomerId(), customer.getEmail(), role);

        return AuthResponseDto.builder()
            .token(token)
            .type("Bearer")
            .customerId(customer.getCustomerId())
            .fullName(customer.getFullName())
            .email(customer.getEmail())
            .role(role)
            .expiresIn(jwtUtil.getExpirationTime() / 1000)
            .build();
    }

    /**
     * Authenticates a user and generates a JWT token.
     * @param dto the login request credentials
     * @return AuthResponseDto containing the JWT token and user info
     * @throws UnauthorizedAccessException if credentials are invalid
     */
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto dto) {
        Customer customer = customerRepository.findByEmail(dto.getEmail())
            .orElseThrow(() -> new UnauthorizedAccessException("Invalid credentials"));

        if (!passwordHasher.verify(dto.getPassword(), customer.getHashedPassword())) {
            throw new UnauthorizedAccessException("Invalid credentials");
        }

        String role = customer.getRole();
        String token = jwtUtil.generateToken(customer.getCustomerId(), customer.getEmail(), role);

        return AuthResponseDto.builder()
            .token(token)
            .type("Bearer")
            .customerId(customer.getCustomerId())
            .fullName(customer.getFullName())
            .email(customer.getEmail())
            .role(role)
            .expiresIn(jwtUtil.getExpirationTime() / 1000)
            .build();
    }

    /**
     * Retrieves customer details by ID.
     * Results are cached for better performance.
     * @param customerId the unique ID of the customer
     * @return CustomerDto containing the customer info
     * @throws ResourceNotFoundException if customer is not found
     */
    @Cacheable(value = "customers", key = "#customerId")
    @Transactional(readOnly = true)
    public CustomerDto getCustomer(String customerId) {
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        return CustomerDto.builder()
            .customerId(customer.getCustomerId())
            .fullName(customer.getFullName())
            .email(customer.getEmail())
            .phone(customer.getPhone())
            .role(customer.getRole())
            .registeredAt(customer.getRegisteredAt())
            .build();
    }

    /**
     * Changes a customer's password.
     * @param customerId the ID of the customer
     * @param dto the password change details
     * @throws ResourceNotFoundException if customer is not found
     * @throws UnauthorizedAccessException if current password is incorrect
     */
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

    /**
     * Allows an admin to change any customer's password.
     * @param adminId the ID of the admin performing the action
     * @param targetCustomerId the ID of the target customer
     * @param newPassword the new password to set
     * @throws ResourceNotFoundException if admin or target customer is not found
     * @throws UnauthorizedAccessException if requester is not an admin
     */
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

    /**
     * Sets up a default admin user if one doesn't exist.
     * This is typically called on application startup.
     */
    @Transactional
    public void setupAdmin() {
        if (customerRepository.findByEmail("admin@orios.com").isEmpty()) {
            Customer admin = Customer.builder()
                .customerId("ADMIN001")
                .fullName("System Administrator")
                .email("admin@orios.com")
                .hashedPassword(passwordHasher.hash("admin123"))
                .phone("0000000000")
                .role("ADMIN")
                .build();
            customerRepository.save(admin);
        }
    }
}
