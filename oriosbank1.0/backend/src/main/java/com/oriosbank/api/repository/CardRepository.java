package com.oriosbank.api.repository;

import com.oriosbank.api.model.Card;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends MongoRepository<Card, String> {
    @org.springframework.data.mongodb.repository.Query("{ 'customer.$id': ?0 }")
    List<Card> findByCustomerCustomerId(String customerId);

    @org.springframework.data.mongodb.repository.Query("{ 'account.$id': ?0 }")
    List<Card> findByAccountAccountId(String accountId);
}
