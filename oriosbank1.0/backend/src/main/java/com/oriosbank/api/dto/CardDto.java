package com.oriosbank.api.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDto {
    private String cardId;
    private String cardNumber; // Masked
    private String cardHolderName;
    private String expiryDate;
    private String cardType;
    private String status;
    private String accountId;
    private LocalDateTime createdAt;
}
