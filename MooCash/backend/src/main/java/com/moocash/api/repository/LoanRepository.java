package com.moocash.api.repository;

import com.moocash.api.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, String> {
    List<Loan> findByCustomerCustomerId(String customerId);

    default List<Loan> findByCustomerId(String customerId) {
        return findByCustomerCustomerId(customerId);
    }
}
