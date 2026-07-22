package com.oriosbank.api.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "customers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @Field("customer_id")
    private String customerId;

    @Field("full_name")
    private String fullName;

    @Indexed(unique = true)
    private String email;

    @Field("hashed_password")
    private String hashedPassword;

    private String phone;
    
    @Builder.Default
    private String role = "USER"; // USER or ADMIN

    @CreatedDate
    @Field("registered_at")
    private LocalDateTime registeredAt;

    @DBRef
    @Builder.Default
    private List<Account> accounts = new ArrayList<>();

    @Version
    private Long version;
}
