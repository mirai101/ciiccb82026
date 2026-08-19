package com.moocash.api;

import com.moocash.api.service.AuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableJpaAuditing
public class MooCashApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(MooCashApiApplication.class, args);
    }

    @Bean
    public CommandLineRunner init(AuthService authService) {
        return args -> authService.setupAdmin();
    }
}
