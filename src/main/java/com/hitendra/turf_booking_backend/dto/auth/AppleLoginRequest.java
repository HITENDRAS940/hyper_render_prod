package com.hitendra.turf_booking_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppleLoginRequest {

    @NotBlank(message = "Identity token is required")
    private String identityToken;
}

