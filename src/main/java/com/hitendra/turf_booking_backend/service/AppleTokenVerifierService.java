package com.hitendra.turf_booking_backend.service;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.hitendra.turf_booking_backend.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.TimeUnit;

/**
 * Dedicated service for verifying Apple identity tokens.
 *
 * Responsibilities:
 * - Decode token header and extract kid
 * - Fetch Apple public keys from JWKS endpoint (with caching)
 * - Verify RSA signature
 * - Validate issuer (https://appleid.apple.com)
 * - Validate audience (app bundle identifier)
 * - Validate expiration
 */
@Service
@Slf4j
public class AppleTokenVerifierService {

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    @Value("${oauth.apple.bundle-id}")
    private String appleBundleId;

    private volatile JwkProvider jwkProvider;

    /**
     * Lazily initialize the JWK provider with caching.
     * Apple's keys rotate infrequently, so caching avoids repeated HTTP calls.
     */
    private JwkProvider getJwkProvider() {
        if (jwkProvider == null) {
            synchronized (this) {
                if (jwkProvider == null) {
                    try {
                        jwkProvider = new JwkProviderBuilder(new URL(APPLE_JWKS_URL))
                                .cached(10, 24, TimeUnit.HOURS)   // cache up to 10 keys for 24 hours
                                .rateLimited(10, 1, TimeUnit.MINUTES) // rate limit JWKS fetches
                                .build();
                    } catch (Exception e) {
                        log.error("Failed to build Apple JWK provider: {}", e.getMessage(), e);
                        throw new AuthException(
                                AuthException.AuthErrorCode.INVALID_OAUTH_TOKEN,
                                "Failed to initialize Apple token verifier"
                        );
                    }
                }
            }
        }
        return jwkProvider;
    }

    /**
     * Verify an Apple identity token and return the decoded JWT.
     *
     * @param identityToken the raw JWT string from Apple Sign-In
     * @return decoded and verified JWT
     * @throws AuthException if verification fails for any reason
     */
    public DecodedJWT verify(String identityToken) {
        try {
            // Step 1: Decode header to extract kid (key ID)
            DecodedJWT unverified = JWT.decode(identityToken);
            String keyId = unverified.getKeyId();

            if (keyId == null || keyId.isBlank()) {
                throw new AuthException(
                        AuthException.AuthErrorCode.INVALID_OAUTH_TOKEN,
                        "Apple identity token missing key ID (kid)"
                );
            }

            // Step 2: Fetch the matching public key from Apple JWKS
            Jwk jwk = getJwkProvider().get(keyId);
            RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();

            // Step 3: Build verifier with all required validations
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(APPLE_ISSUER)            // iss must be https://appleid.apple.com
                    .withAudience(appleBundleId)          // aud must match bundle ID
                    .acceptLeeway(30)                     // 30-second clock skew tolerance
                    .build();

            // Step 4: Verify signature + all claims (iss, aud, exp)
            DecodedJWT verified = verifier.verify(identityToken);

            log.info("Apple identity token verified successfully — sub: {}", verified.getSubject());
            return verified;

        } catch (AuthException e) {
            throw e; // Re-throw our own exceptions as-is
        } catch (JWTVerificationException e) {
            log.error("Apple token verification failed: {}", e.getMessage());
            throw new AuthException(
                    AuthException.AuthErrorCode.INVALID_OAUTH_TOKEN,
                    "Invalid Apple identity token: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error verifying Apple token: {}", e.getMessage(), e);
            throw new AuthException(
                    AuthException.AuthErrorCode.INVALID_OAUTH_TOKEN,
                    "Failed to verify Apple identity token: " + e.getMessage()
            );
        }
    }
}

