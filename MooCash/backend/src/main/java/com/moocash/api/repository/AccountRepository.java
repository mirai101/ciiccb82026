package com.moocash.api.repository;

import com.moocash.api.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    List<Account> findByCustomerCustomerId(String customerId);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM Account a WHERE a.customer.customerId = :customerId")
    BigDecimal getTotalBalanceByCustomerId(@Param("customerId") String customerId);
}
