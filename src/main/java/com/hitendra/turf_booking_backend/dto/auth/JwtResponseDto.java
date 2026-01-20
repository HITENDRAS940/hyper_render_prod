package com.hitendra.turf_booking_backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponseDto {
    private String token;
    private boolean isNewUser;  // true if this is a new registration, false if existing user login
}

