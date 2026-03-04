package com.hitendra.turf_booking_backend.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.hitendra.turf_booking_backend.dto.auth.AppleLoginRequest;
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
     * IMPORTANT: Apple provides email and fullName ONLY on first login.
     * On subsequent logins, these fields are null in the request body.
     * We MUST save them to the database on first login so they persist.
     *
     * @param request AppleLoginRequest containing identityToken, email, and fullName (first login only)
     * @return OAuthResponseDto with application JWT and user info
     */
    public OAuthResponseDto authenticate(AppleLoginRequest request) {

        String identityToken = request.getIdentityToken();

        // ── Step 1: Verify token ──────────────────────────────────────────
        DecodedJWT verified = appleTokenVerifierService.verify(identityToken);

        // ── Step 2: Extract claims ────────────────────────────────────────
        String appleUserId = verified.getSubject(); // sub — always present

        // Priority 1: Use email from request body (first login only)
        // Priority 2: Fall back to JWT claim (subsequent logins)
        String email = request.getEmail();
        if (email == null || email.isBlank()) {
            if (verified.getClaim("email") != null && !verified.getClaim("email").isNull()) {
                email = verified.getClaim("email").asString();
            }
        }

        // Priority 1: Use fullName from request body (first login only)
        // Priority 2: Extract from JWT claims (subsequent logins may have embedded names)
        // Priority 3: Use empty if neither available
        String fullName = null;

        if (request.getFullName() != null) {
            // First login - use fullName from request body
            AppleLoginRequest.FullName nameObj = request.getFullName();
            String givenName = nameObj.getGivenName();
            String familyName = nameObj.getFamilyName();
            fullName = buildFullName(givenName, familyName);
            log.info("Extracted fullName from request body: {}", fullName);
        } else {
            // Subsequent logins - try to extract from JWT claims
            String givenName = null;
            String familyName = null;
            if (verified.getClaim("givenName") != null && !verified.getClaim("givenName").isNull()) {
                givenName = verified.getClaim("givenName").asString();
            }
            if (verified.getClaim("familyName") != null && !verified.getClaim("familyName").isNull()) {
                familyName = verified.getClaim("familyName").asString();
            }
            fullName = buildFullName(givenName, familyName);
            if (fullName != null) {
                log.info("Extracted fullName from JWT claims: {}", fullName);
            }
        }

        log.info("Apple authentication — sub: {}, email: {}, fullName: {}", appleUserId, email, fullName);

        // ── Step 3: Find or create user ───────────────────────────────────
        User user;
        boolean isNewUser = false;

        // CASE 1: Look up by Apple provider ID (returning user)
        Optional<User> byProvider = userRepository.findByOauthProviderIdAndOauthProvider(
                appleUserId, OAuthProvider.APPLE);

        if (byProvider.isPresent()) {
            user = byProvider.get();

            // Update name if provided and user doesn't have one yet
            if (fullName != null && (user.getName() == null || user.getName().isBlank())) {
                user.setName(fullName);
                user = userRepository.save(user);
                log.info("Updated name for existing Apple user — userId: {}, name: {}", user.getId(), fullName);
            }

            // Update email if provided and different from stored email
            if (email != null && !email.equals(user.getEmail())) {
                user.setEmail(email);
                user = userRepository.save(user);
                log.info("Updated email for existing Apple user — userId: {}, email: {}", user.getId(), email);
            }

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

                // Update name if provided and user doesn't have one
                if (fullName != null && (user.getName() == null || user.getName().isBlank())) {
                    user.setName(fullName);
                }
                user = userRepository.save(user);
                log.info("Linked Apple provider to existing user — userId: {}, email: {}", user.getId(), email);

            } else {
                // Create brand-new user
                isNewUser = true;
                user = User.builder()
                        .email(email)
                        .name(fullName) // Set the full name from request body or JWT
                        .oauthProvider(OAuthProvider.APPLE)
                        .oauthProviderId(appleUserId)
                        .emailVerified(true)
                        .role(Role.USER)
                        .enabled(true)
                        .createdAt(Instant.now())
                        .build();
                user = userRepository.save(user);
                log.info("Created new Apple user — userId: {}, email: {}, name: {}", user.getId(), email, fullName);
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

    /**
     * Helper method to build full name from givenName and familyName.
     *
     * @param givenName First name from Apple claims
     * @param familyName Last name from Apple claims
     * @return Combined full name, or null if both are empty
     */
    private String buildFullName(String givenName, String familyName) {
        if (givenName == null && familyName == null) {
            return null;
        }

        StringBuilder fullName = new StringBuilder();
        if (givenName != null && !givenName.isBlank()) {
            fullName.append(givenName);
        }
        if (familyName != null && !familyName.isBlank()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(familyName);
        }

        return fullName.length() > 0 ? fullName.toString() : null;
    }
}

