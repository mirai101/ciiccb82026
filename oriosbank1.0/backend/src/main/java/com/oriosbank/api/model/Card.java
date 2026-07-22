package com.oriosbank.api.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @Field("card_id")
    private String cardId;

    private String cardNumber; // Masked in DTO
    private String cardHolderName;
    private String expiryDate;
    private String cvv;
    private String cardType; // OriosVISA, OriosMASTER
    
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, BLOCKED

    @DBRef
    private Account account;

    @DBRef
    private Customer customer;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;
}
