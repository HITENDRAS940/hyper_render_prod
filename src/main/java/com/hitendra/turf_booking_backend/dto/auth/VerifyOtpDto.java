package com.hitendra.turf_booking_backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpDto {
    @NotBlank
    private String phone;
    @NotBlank
    private String otp;
}

