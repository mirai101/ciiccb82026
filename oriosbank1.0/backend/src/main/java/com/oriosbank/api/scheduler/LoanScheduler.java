package com.oriosbank.api.scheduler;

import com.oriosbank.api.service.LoanService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LoanScheduler {

    private final LoanService loanService;

    public LoanScheduler(LoanService loanService) {
        this.loanService = loanService;
    }

    // Run every hour
    @Scheduled(fixedRate = 3600000)
    public void processAutoDebts() {
        System.out.println("Running scheduled auto-debt processing...");
        loanService.processAutoDebts();
    }
}
