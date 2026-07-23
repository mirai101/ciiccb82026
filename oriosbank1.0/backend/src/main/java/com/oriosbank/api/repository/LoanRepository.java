package com.oriosbank.api.repository;

import com.oriosbank.api.model.Loan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoanRepository extends MongoRepository<Loan, String> {
    @Query("{ 'customer.$id': ?0 }")
    List<Loan> findByCustomerId(String customerId);

    @Query("{ 'customer.$id': ?0, 'createdAt': { $gte: ?1 } }")
    long countByCustomerIdAndCreatedAtAfter(String customerId, LocalDateTime since);
}
