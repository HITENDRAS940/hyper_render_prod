package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.auth.*;
import com.hitendra.turf_booking_backend.service.AuthService;
import com.hitendra.turf_booking_backend.service.EmailOtpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication APIs")
public class AuthController {

    private final AuthService authService;
    private final EmailOtpService emailOtpService;

    @PostMapping("/request-otp")
    @Operation(summary = "Request OTP", description = "Send OTP to user's phone number")
    public ResponseEntity<String> requestOtp(@Valid @RequestBody RequestOtpDto request) {
        authService.requestOtp(request);
        return ResponseEntity.ok("OTP sent successfully");
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Verify OTP", description = "Verify OTP and get JWT token")
    public ResponseEntity<JwtResponseDto> verifyOtp(
            @Valid @RequestBody VerifyOtpDto request
    ) {
        JwtResponseDto response = authService.verifyOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/request-email-otp")
    @Operation(summary = "Request Email OTP", description = "Send OTP to user's email address")
    public ResponseEntity<String> requestEmailOtp(@Valid @RequestBody RequestEmailOtpDto request) {
        emailOtpService.requestEmailOtp(request);
        return ResponseEntity.ok("OTP sent successfully to your email");
    }

    @PostMapping("/verify-email-otp")
    @Operation(summary = "Verify Email OTP", description = "Verify email OTP and get JWT token")
    public ResponseEntity<JwtResponseDto> verifyEmailOtp(
            @Valid @RequestBody VerifyEmailOtpDto request
    ) {
        JwtResponseDto response = emailOtpService.verifyEmailOtp(request);
        return ResponseEntity.ok(response);
    }
}
