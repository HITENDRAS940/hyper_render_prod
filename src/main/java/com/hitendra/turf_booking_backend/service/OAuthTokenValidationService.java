package com.hitendra.turf_booking_backend.service;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hitendra.turf_booking_backend.entity.OAuthProvider;
import com.hitendra.turf_booking_backend.exception.AuthException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;

@Service
@Slf4j
public class OAuthTokenValidationService {

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    @Value("${oauth.apple.bundle-id}")
    private String appleBundleId;

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";

    /**
     * Validate Google ID token and extract user information
     */
    public OAuthUserInfo validateGoogleToken(String idToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);

            if (googleIdToken == null) {
                log.error("Invalid Google ID token");
                throw new AuthException(
                    AuthException.AuthErrorCode.INVALID_OAUTH_TOKEN,
                    "Invalid Google ID token"
                );
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email = payload.getEmail();
            boolean emailVerified = payload.getEmailVerified();
            String userId = payload.getSubject();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            if (!emailVerified) {
                log.error("Google email not verified for: {}", email);
                throw new AuthException(
                    AuthException.AuthErrorCode.EMAIL_NOT_VERIFIED,
                    "Email is not verified by Google"
                );
            }

            log.info("Google token validated successfully for email: {}", email);
            return new OAuthUserInfo(
                email,
                name,
                userId,
                OAuthProvider.GOOGLE,
                emailVerified,
                pictureUrl
            );

        } catch (Exception e) {
            log.error("Error validating Google token: {}", e.getMessage(), e);
            throw new AuthException(
                AuthException.AuthErrorCode.INVALID_OAUTH_TOKEN,
                "Failed to validate Google token: " + e.getMessage()
            );
        }
    }

    /**
     * Validate Apple ID token and extract user information
     * Apple uses JWT tokens signed with their public keys
     */
    public OAuthUserInfo validateAppleToken(String idToken) {
        try {
            // Decode the JWT without verification first to get the key ID
            DecodedJWT jwt = JWT.decode(idToken);
            String keyId = jwt.getKeyId();

            // Fetch Apple's public keys
            JwkProvider provider = new UrlJwkProvider(new URL(APPLE_JWKS_URL));
            Jwk jwk = provider.get(keyId);

            // Get the public key
            RSAPublicKey publicKey = (RSAPublicKey) jwk.getPublicKey();

            // Verify the token
            Algorithm algorithm = Algorithm.RSA256(publicKey, null);
            algorithm.verify(jwt);

            // Extract claims
            String email = jwt.getClaim("email").asString();
            Boolean emailVerified = jwt.getClaim("email_verified").asBoolean();
            String userId = jwt.getSubject(); // 'sub' claim
            String audience = jwt.getAudience().get(0);

            // Verify audience matches your app's bundle ID
            if (!appleBundleId.equals(audience)) {
                log.error("Apple token audience mismatch. Expected: {}, Got: {}", appleBundleId, audience);
                throw new AuthException(
                    AuthException.AuthErrorCode.INVALID_OAUTH_TOKEN,
                    "Invalid Apple token audience"
                );
            }

            // Apple may provide email as private relay (e.g., privaterelay.appleid.com)
            boolean isPrivateEmail = email != null && email.contains("privaterelay.appleid.com");

            log.info("Apple token validated successfully for email: {} (private: {})", email, isPrivateEmail);

            return new OAuthUserInfo(
                email,
                null, // Apple doesn't include name in token (only on first sign-in)
                userId,
                OAuthProvider.APPLE,
                emailVerified != null ? emailVerified : true, // Apple emails are considered verified
                null
            );

        } catch (Exception e) {
            log.error("Error validating Apple token: {}", e.getMessage(), e);
            throw new AuthException(
                AuthException.AuthErrorCode.INVALID_OAUTH_TOKEN,
                "Failed to validate Apple token: " + e.getMessage()
            );
        }
    }

    /**
     * Validate OAuth token based on provider
     */
    public OAuthUserInfo validateToken(String idToken, OAuthProvider provider) {
        return switch (provider) {
            case GOOGLE -> validateGoogleToken(idToken);
            case APPLE -> validateAppleToken(idToken);
        };
    }

    /**
     * Data class to hold OAuth user information
     */
    public record OAuthUserInfo(
        String email,
        String name,
        String providerId,
        OAuthProvider provider,
        boolean emailVerified,
        String pictureUrl
    ) {}
}

