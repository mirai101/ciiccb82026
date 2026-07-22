package com.oriosbank.api.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Document(collection = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @Field("transaction_id")
    private String transactionId;

    private String type;

    private Double amount;

    @CreatedDate
    @Field("timestamp")
    private LocalDateTime timestamp;

    @DBRef
    @Field("account")
    private Account account;

    @Field("from_account")
    private String fromAccount;

    @Field("to_account")
    private String toAccount;

    private String description;
}
