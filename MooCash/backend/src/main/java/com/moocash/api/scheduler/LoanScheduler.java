package com.moocash.api.scheduler;

import com.moocash.api.service.LoanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoanScheduler {

    private final LoanService loanService;

    public LoanScheduler(LoanService loanService) {
        this.loanService = loanService;
    }

    // Run every hour
    @Scheduled(fixedRate = 3600000)
    public void processAutoDebts() {
        log.info("Running scheduled auto-debt processing...");
        loanService.processAutoDebts();
    }
}
