package com.moocash.api.repository;

import com.moocash.api.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByAccountAccountId(String accountId);
    List<Transaction> findByFromAccountOrToAccount(String fromAccount, String toAccount);

    long countByAccountAccountIdInAndTypeAndTimestampGreaterThanEqual(
            List<String> accountIds, String type, LocalDateTime start);
}
