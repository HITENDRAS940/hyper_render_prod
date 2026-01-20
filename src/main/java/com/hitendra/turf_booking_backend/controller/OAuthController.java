package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.auth.OAuthLoginDto;
import com.hitendra.turf_booking_backend.dto.auth.OAuthResponseDto;
import com.hitendra.turf_booking_backend.service.OAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/oauth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OAuth Authentication", description = "OAuth 2.0 authentication APIs for Google and Apple Sign-In")
public class OAuthController {

    private final OAuthService oAuthService;

    @PostMapping("/login")
    @Operation(
        summary = "OAuth Login (Google/Apple)",
        description = "Authenticate user with Google or Apple ID token. " +
                     "Backend validates the token, finds or creates user by email, and returns JWT. " +
                     "The returned JWT is used for all subsequent API calls."
    )
    public ResponseEntity<OAuthResponseDto> oauthLogin(
            @Valid @RequestBody OAuthLoginDto request
    ) {
        log.info("OAuth login request - Provider: {}", request.getProvider());
        OAuthResponseDto response = oAuthService.handleOAuthLogin(request);
        log.info("OAuth login successful - Email: {}, IsNewUser: {}",
                response.getEmail(), response.isNewUser());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    @Operation(
        summary = "Test OAuth Endpoint",
        description = "Simple test endpoint to verify OAuth routes are accessible"
    )
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("OAuth endpoints are working! Available: POST /auth/oauth/login");
    }
}

