package com.hitendra.turf_booking_backend.entity;

/**
 * Payment status enum for tracking payment lifecycle.
 * Used in conjunction with BookingStatus for complete state management.
 */
public enum PaymentStatus {
    NOT_STARTED,      // Payment not yet initiated
    IN_PROGRESS,      // Razorpay order created, awaiting user payment
    SUCCESS,          // Payment captured successfully
    FAILED            // Payment failed or cancelled
}

