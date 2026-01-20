package com.hitendra.turf_booking_backend.dto.auth;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthLoginDto {
    @NotBlank(message = "ID token is required")
    private String idToken;
    @NotBlank(message = "Provider is required (GOOGLE or APPLE)")
    private String provider;
    private String fullName;
}
