package com.moocash.api.repository;

import com.moocash.api.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, String> {
    List<Card> findByCustomerCustomerId(String customerId);
    List<Card> findByAccountAccountId(String accountId);
}
