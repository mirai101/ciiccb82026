package com.moocash.api.model;

import lombok.*;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @Column(name = "card_id", length = 36)
    private String cardId;

    @Column(name = "card_number")
    private String cardNumber; // Masked in DTO
    private String cardHolderName;
    private String expiryDate;
    private String cvv;
    private String cardType; // MooCashVISA, MooCashMASTER

    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, BLOCKED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
