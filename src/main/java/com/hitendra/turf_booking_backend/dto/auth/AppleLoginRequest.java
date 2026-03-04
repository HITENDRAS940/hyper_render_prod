package com.hitendra.turf_booking_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppleLoginRequest {

    @NotBlank(message = "Identity token is required")
    private String identityToken;

    private FullName fullName;

    private String email;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FullName {
        private String givenName;
        private String familyName;
    }
}

