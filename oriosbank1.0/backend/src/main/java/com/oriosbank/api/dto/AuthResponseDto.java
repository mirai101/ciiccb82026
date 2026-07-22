package com.oriosbank.api.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDto {
    private String token;
    private String type;
    private String customerId;
    private String fullName;
    private String email;
    private String role;
    private Long expiresIn;
}
