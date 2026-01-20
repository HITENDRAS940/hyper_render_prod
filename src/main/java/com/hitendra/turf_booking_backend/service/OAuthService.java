package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.auth.OAuthLoginDto;
import com.hitendra.turf_booking_backend.dto.auth.OAuthResponseDto;
import com.hitendra.turf_booking_backend.entity.OAuthProvider;
import com.hitendra.turf_booking_backend.entity.Role;
import com.hitendra.turf_booking_backend.entity.User;
import com.hitendra.turf_booking_backend.exception.AuthException;
import com.hitendra.turf_booking_backend.repository.UserRepository;
import com.hitendra.turf_booking_backend.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OAuthService {

    private final OAuthTokenValidationService tokenValidationService;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    /**
     * Handle OAuth login (Google or Apple)
     * Flow:
     * 1. Validate OAuth token with provider
     * 2. Extract email from token
     * 3. Find or create user by email
     * 4. Generate JWT token
     * 5. Return JWT to client
     */
    public OAuthResponseDto handleOAuthLogin(OAuthLoginDto request) {
        // Parse provider
        OAuthProvider provider;
        try {
            provider = OAuthProvider.valueOf(request.getProvider().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AuthException(
                AuthException.AuthErrorCode.INVALID_OAUTH_TOKEN,
                "Invalid provider. Must be GOOGLE or APPLE"
            );
        }

        // Validate token with OAuth provider
        OAuthTokenValidationService.OAuthUserInfo userInfo =
            tokenValidationService.validateToken(request.getIdToken(), provider);

        String email = userInfo.email();
        String name = request.getFullName() != null ? request.getFullName() : userInfo.name();
        String providerId = userInfo.providerId();

        log.info("OAuth login attempt - Provider: {}, Email: {}, Name: {}", provider, email, name);

        // Check if user exists by email
        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        boolean isNewUser = false;

        if (existingUser.isPresent()) {
            // EXISTING USER - Login flow
            user = existingUser.get();
            log.info("Existing user found - UserId: {}, Email: {}", user.getId(), email);

            // Update OAuth provider info if not set or different
            if (user.getOauthProvider() == null || !user.getOauthProvider().equals(provider)) {
                user.setOauthProvider(provider);
                user.setOauthProviderId(providerId);
                user.setEmailVerified(true);
                userRepository.save(user);
                log.info("Updated OAuth info for existing user: {}", user.getId());
            }

            // If user has no name but OAuth provides one, update it
            if (user.getName() == null && name != null) {
                user.setName(name);
                userRepository.save(user);
            }

        } else {
            // NEW USER - Registration flow
            isNewUser = true;

            // Create new user with OAuth info
            user = User.builder()
                    .email(email)
                    .name(name)
                    .oauthProvider(provider)
                    .oauthProviderId(providerId)
                    .emailVerified(true)
                    .role(Role.USER)
                    .enabled(true)
                    .createdAt(Instant.now())
                    .build();

            user = userRepository.save(user);
            log.info("Created new OAuth user - UserId: {}, Email: {}, Provider: {}",
                user.getId(), email, provider);
        }

        // Generate JWT token (email-based)
        String jwtToken = jwtUtils.generateTokenFromEmail(
            user.getEmail(),
            "ROLE_" + user.getRole().name(),
            user.getId(),
            user.getName()
        );

        return OAuthResponseDto.builder()
                .token(jwtToken)
                .isNewUser(isNewUser)
                .email(email)
                .name(user.getName())
                .provider(provider.name())
                .build();
    }
}

