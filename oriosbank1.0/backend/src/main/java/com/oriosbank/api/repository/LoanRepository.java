package com.oriosbank.api.repository;

import com.oriosbank.api.model.Loan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends MongoRepository<Loan, String> {
    @org.springframework.data.mongodb.repository.Query("{ 'customer.$id': ?0 }")
    List<Loan> findByCustomerId(String customerId);
}
