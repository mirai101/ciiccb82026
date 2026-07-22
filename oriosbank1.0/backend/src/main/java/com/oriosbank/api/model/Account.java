package com.oriosbank.api.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class Account {

    @Id
    @Field("account_id")
    private String accountId;

    @DBRef
    @Field("customer")
    private Customer customer;

    private Double balance;

    private String status = "ACTIVE"; // ACTIVE, BLOCKED, HELD

    private boolean isHidden = false;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @DBRef
    private List<Transaction> transactions = new ArrayList<>();

    @Version
    private Long version;

    public abstract String getAccountType();
    public abstract double getInterestRate();
}
