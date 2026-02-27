package com.hitendra.turf_booking_backend.service;

import com.auth0.jwt.interfaces.DecodedJWT;
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

/**
 * Dedicated service for Apple Sign-In authentication.
 *
 * Flow:
 * 1. Verify Apple identity token (signature, issuer, audience, expiration)
 * 2. Extract claims (sub, email)
 * 3. Find or create user with proper account linking
 * 4. Generate application JWT
 * 5. Return AuthResponse
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AppleAuthService {

    private final AppleTokenVerifierService appleTokenVerifierService;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    /**
     * Authenticate a user with an Apple identity token.
     *
     * @param identityToken the raw JWT from Apple Sign-In
     * @return OAuthResponseDto with application JWT and user info
     */
    public OAuthResponseDto authenticate(String identityToken) {
        // ── Step 1: Verify token ──────────────────────────────────────────
        DecodedJWT verified = appleTokenVerifierService.verify(identityToken);

        // ── Step 2: Extract claims ────────────────────────────────────────
        String appleUserId = verified.getSubject(); // sub — always present
        String email = verified.getClaim("email").asString(); // may be null after first login

        log.info("Apple authentication — sub: {}, email: {}", appleUserId, email);

        // ── Step 3: Find or create user ───────────────────────────────────
        User user;
        boolean isNewUser = false;

        // CASE 1: Look up by Apple provider ID (returning user)
        Optional<User> byProvider = userRepository.findByOauthProviderIdAndOauthProvider(
                appleUserId, OAuthProvider.APPLE);

        if (byProvider.isPresent()) {
            user = byProvider.get();
            log.info("Existing Apple user found — userId: {}, email: {}", user.getId(), user.getEmail());

        } else if (email != null && !email.isBlank()) {
            // CASE 2: Email available — check if an account with this email exists
            Optional<User> byEmail = userRepository.findByEmail(email);

            if (byEmail.isPresent()) {
                // Link Apple provider to existing account
                user = byEmail.get();
                user.setOauthProvider(OAuthProvider.APPLE);
                user.setOauthProviderId(appleUserId);
                user.setEmailVerified(true);
                user = userRepository.save(user);
                log.info("Linked Apple provider to existing user — userId: {}, email: {}", user.getId(), email);

            } else {
                // Create brand-new user
                isNewUser = true;
                user = User.builder()
                        .email(email)
                        .oauthProvider(OAuthProvider.APPLE)
                        .oauthProviderId(appleUserId)
                        .emailVerified(true)
                        .role(Role.USER)
                        .enabled(true)
                        .createdAt(Instant.now())
                        .build();
                user = userRepository.save(user);
                log.info("Created new Apple user — userId: {}, email: {}", user.getId(), email);
            }

        } else {
            // CASE 3: Email is null AND no existing provider record → reject
            log.error("Apple authentication failed — email is null and no existing provider record for sub: {}", appleUserId);
            throw new AuthException(
                    AuthException.AuthErrorCode.USER_NOT_FOUND,
                    "Unable to authenticate with Apple. No email provided and no existing account found for this Apple ID."
            );
        }

        // ── Step 4: Generate application JWT ──────────────────────────────
        String jwtToken = jwtUtils.generateTokenFromEmail(
                user.getEmail(),
                "ROLE_" + user.getRole().name(),
                user.getId(),
                user.getName(),
                user.getPhone()
        );

        // ── Step 5: Return response ──────────────────────────────────────
        return OAuthResponseDto.builder()
                .token(jwtToken)
                .isNewUser(isNewUser)
                .email(user.getEmail())
                .name(user.getName())
                .provider(OAuthProvider.APPLE.name())
                .build();
    }
}

