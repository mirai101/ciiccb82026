package com.oriosbank.api;

import com.oriosbank.api.service.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for OriosBank API.
 * Configures Spring Boot, caching, and scheduling.
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class OriosBankApiApplication {
    /**
     * Entry point of the application.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(OriosBankApiApplication.class, args);
    }

    /**
     * Initializes the application with a default admin user.
     * @param authService the authentication service to set up admin
     * @return CommandLineRunner instance
     */
    @Bean
    public CommandLineRunner init(AuthService authService) {
        return args -> authService.setupAdmin();
    }
}
