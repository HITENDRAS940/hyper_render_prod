package com.hitendra.turf_booking_backend.exception;

import lombok.Getter;

/**
 * Custom exception for authentication-related errors
 */
@Getter
public class AuthException extends RuntimeException {

    private final AuthErrorCode errorCode;

    public AuthException(AuthErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public enum AuthErrorCode {
        INVALID_OTP,
        EXPIRED_OTP,
        OTP_NOT_FOUND,
        PHONE_INVALID,
        TOKEN_INVALID,
        TOKEN_EXPIRED,
        INVALID_OAUTH_TOKEN,
        EMAIL_NOT_VERIFIED,
        USER_NOT_FOUND,
        PHONE_ALREADY_EXISTS,
        EMAIL_ALREADY_EXISTS
    }
}

