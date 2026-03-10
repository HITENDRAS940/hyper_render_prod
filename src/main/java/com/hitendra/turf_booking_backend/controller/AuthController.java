package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.auth.*;
import com.hitendra.turf_booking_backend.service.AppleAuthService;
import com.hitendra.turf_booking_backend.service.AuthService;
import com.hitendra.turf_booking_backend.service.EmailOtpService;
import com.hitendra.turf_booking_backend.util.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "User authentication APIs")
public class AuthController {

    private final AuthService authService;
    private final EmailOtpService emailOtpService;
    private final AppleAuthService appleAuthService;
    private final JwtUtils jwtUtils;

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
    @Operation(
        summary = "Request Email OTP",
        description = "Send OTP to user's email address. For test accounts (googletest@hyper.com, razorpaytest@hyper.com), returns JWT token directly without OTP."
    )
    public ResponseEntity<EmailOtpResponseDto> requestEmailOtp(@Valid @RequestBody RequestEmailOtpDto request) {
        EmailOtpResponseDto response = emailOtpService.requestEmailOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email-otp")
    @Operation(summary = "Verify Email OTP", description = "Verify email OTP and get JWT token")
    public ResponseEntity<JwtResponseDto> verifyEmailOtp(
            @Valid @RequestBody VerifyEmailOtpDto request
    ) {
        JwtResponseDto response = emailOtpService.verifyEmailOtp(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh JWT Token",
        description = "Exchange a valid (or recently expired) JWT for a brand-new one with a fresh expiration. " +
                      "Pass the current token in the Authorization header as 'Bearer <token>'."
    )
    public ResponseEntity<Map<String, String>> refreshToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Missing or invalid Authorization header"));
        }
        String token = header.substring(7);
        if (!jwtUtils.validateJwtToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token is invalid or expired"));
        }
        String newToken = jwtUtils.refreshToken(token);
        log.info("Token refreshed for user: {}", jwtUtils.getEmailFromJwtToken(newToken));
        return ResponseEntity.ok(Map.of("token", newToken));
    }

    @PostMapping("/apple")
    @Operation(
        summary = "Sign in with Apple",
        description = "Authenticate user with Apple identity token. " +
                     "Backend verifies the token signature using Apple public keys, " +
                     "validates issuer/audience/expiration, finds or creates user, and returns JWT."
    )
    public ResponseEntity<OAuthResponseDto> loginWithApple(
            @Valid @RequestBody AppleLoginRequest request
    ) {
        log.info("Apple Sign-In request received");
        OAuthResponseDto response = appleAuthService.authenticate(request);
        log.info("Apple Sign-In successful — email: {}, isNewUser: {}",
                response.getEmail(), response.isNewUser());
        return ResponseEntity.ok(response);
    }
}
