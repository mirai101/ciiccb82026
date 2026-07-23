package com.oriosbank.api.repository;

import com.oriosbank.api.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    @Query("{ 'account.$id': ?0 }")
    List<Transaction> findByAccountAccountId(String accountId);
    List<Transaction> findByFromAccountOrToAccount(String fromAccount, String toAccount);

    @Query("{ 'account.$id': { $in: ?0 }, 'type': ?1, 'timestamp': { $gte: ?2 } }")
    long countByAccountIdsAndTypeAndTimestampAfter(List<String> accountIds, String type, LocalDateTime since);
}
