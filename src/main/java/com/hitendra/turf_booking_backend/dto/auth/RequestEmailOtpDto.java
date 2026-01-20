package com.hitendra.turf_booking_backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestEmailOtpDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
}

