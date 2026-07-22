package com.oriosbank.api.repository;

import com.oriosbank.api.model.Account;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends MongoRepository<Account, String> {
    @org.springframework.data.mongodb.repository.Query("{ 'customer.$id': ?0 }")
    List<Account> findByCustomerCustomerId(String customerId);

    default double getTotalBalanceByCustomerId(String customerId) {
        return findByCustomerCustomerId(customerId).stream()
                .mapToDouble(Account::getBalance)
                .sum();
    }
}
