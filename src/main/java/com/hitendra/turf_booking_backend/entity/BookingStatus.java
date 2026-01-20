package com.hitendra.turf_booking_backend.entity;

/**
 * Booking lifecycle status.
 * Used with PaymentStatus for complete state management.
 */
public enum BookingStatus {
    PENDING,                // Initial state after booking creation
    PAYMENT_PENDING,        // Legacy status for backward compatibility
    AWAITING_CONFIRMATION,  // Payment in progress, awaiting webhook confirmation
    CONFIRMED,              // Payment confirmed, booking is active
    COMPLETED,              // Service has been delivered
    CANCELLED,              // System/admin cancelled or payment failed
    CANCELLED_BY_USER,      // User-initiated cancellation with potential refund
    EXPIRED,                // Payment timeout expired
    REFUNDED                // Payment was refunded (legacy - use CANCELLED_BY_USER)
}

