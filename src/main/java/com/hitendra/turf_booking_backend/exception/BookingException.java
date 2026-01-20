package com.hitendra.turf_booking_backend.exception;

import lombok.Getter;

/**
 * Custom exception for booking-related errors.
 * Provides detailed error information for proper API responses.
 */
@Getter
public class BookingException extends RuntimeException {

    private final BookingErrorCode errorCode;
    private final String details;

    public BookingException(String message) {
        super(message);
        this.errorCode = BookingErrorCode.BOOKING_ERROR;
        this.details = null;
    }

    public BookingException(String message, BookingErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public BookingException(String message, BookingErrorCode errorCode, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public BookingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = BookingErrorCode.BOOKING_ERROR;
        this.details = null;
    }

    /**
     * Error codes for different booking failure scenarios.
     */
    public enum BookingErrorCode {
        // Validation errors (400)
        INVALID_REQUEST,
        INVALID_DATE,
        INVALID_TIME_RANGE,
        INVALID_SLOT,

        // Not found errors (404)
        SERVICE_NOT_FOUND,
        ACTIVITY_NOT_FOUND,
        RESOURCE_NOT_FOUND,
        SLOT_NOT_FOUND,
        BOOKING_NOT_FOUND,

        // Availability errors (409)
        SLOT_UNAVAILABLE,
        NO_RESOURCES_AVAILABLE,
        SERVICE_UNAVAILABLE,
        ACTIVITY_UNAVAILABLE,
        RESOURCE_DISABLED,
        SLOT_DISABLED,

        // Conflict errors (409)
        BOOKING_ALREADY_EXISTS,
        BOOKING_ALREADY_CANCELLED,
        BOOKING_ALREADY_COMPLETED,
        IDEMPOTENCY_CONFLICT,

        // Payment errors (402)
        INSUFFICIENT_FUNDS,
        PAYMENT_FAILED,

        // Server errors (500)
        BOOKING_ERROR,
        INTERNAL_ERROR
    }

    /**
     * Get HTTP status code based on error code.
     */
    public int getHttpStatus() {
        return switch (errorCode) {
            case INVALID_REQUEST, INVALID_DATE, INVALID_TIME_RANGE, INVALID_SLOT -> 400;
            case SERVICE_NOT_FOUND, ACTIVITY_NOT_FOUND, RESOURCE_NOT_FOUND,
                 SLOT_NOT_FOUND, BOOKING_NOT_FOUND -> 404;
            case SLOT_UNAVAILABLE, NO_RESOURCES_AVAILABLE, SERVICE_UNAVAILABLE,
                 ACTIVITY_UNAVAILABLE, RESOURCE_DISABLED, SLOT_DISABLED,
                 BOOKING_ALREADY_EXISTS, BOOKING_ALREADY_CANCELLED,
                 BOOKING_ALREADY_COMPLETED, IDEMPOTENCY_CONFLICT -> 409;
            case INSUFFICIENT_FUNDS, PAYMENT_FAILED -> 402;
            case BOOKING_ERROR, INTERNAL_ERROR -> 500;
        };
    }
}

