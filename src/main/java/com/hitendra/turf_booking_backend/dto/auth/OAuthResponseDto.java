package com.hitendra.turf_booking_backend.dto.auth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthResponseDto {
    private String token;
    private boolean isNewUser;
    private String email;
    private String name;
    private String provider;
}
