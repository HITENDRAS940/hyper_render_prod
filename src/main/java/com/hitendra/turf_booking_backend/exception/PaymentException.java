package com.hitendra.turf_booking_backend.exception;

import lombok.Getter;

/**
 * Custom exception for payment-related errors.
 * Provides detailed error information for proper API responses.
 */
@Getter
public class PaymentException extends RuntimeException {

    private final PaymentErrorCode errorCode;
    private final String details;

    public PaymentException(String message) {
        super(message);
        this.errorCode = PaymentErrorCode.PAYMENT_ERROR;
        this.details = null;
    }

    public PaymentException(String message, PaymentErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public PaymentException(String message, PaymentErrorCode errorCode, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = PaymentErrorCode.PAYMENT_ERROR;
        this.details = null;
    }

    /**
     * Error codes for different payment failure scenarios.
     */
    public enum PaymentErrorCode {
        // Validation errors (400)
        INVALID_PAYMENT_REQUEST,

        // Expired booking error (410)
        BOOKING_EXPIRED,

        // Conflict errors (409)
        SLOT_ALREADY_LOCKED,
        DUPLICATE_ORDER,
        ORDER_ALREADY_EXISTS,

        // Payment creation errors (402)
        RAZORPAY_ORDER_FAILED,
        PAYMENT_SIGNATURE_INVALID,

        // Not found errors (404)
        BOOKING_NOT_FOUND,
        ORDER_NOT_FOUND,
        PAYMENT_NOT_FOUND,

        // Server errors (500)
        PAYMENT_ERROR,
        INTERNAL_ERROR
    }

    /**
     * Get HTTP status code based on error code.
     */
    public int getHttpStatus() {
        return switch (errorCode) {
            case INVALID_PAYMENT_REQUEST -> 400;
            case BOOKING_EXPIRED -> 410;
            case SLOT_ALREADY_LOCKED, DUPLICATE_ORDER, ORDER_ALREADY_EXISTS -> 409;
            case RAZORPAY_ORDER_FAILED, PAYMENT_SIGNATURE_INVALID -> 402;
            case BOOKING_NOT_FOUND, ORDER_NOT_FOUND, PAYMENT_NOT_FOUND -> 404;
            case PAYMENT_ERROR, INTERNAL_ERROR -> 500;
        };
    }
}

