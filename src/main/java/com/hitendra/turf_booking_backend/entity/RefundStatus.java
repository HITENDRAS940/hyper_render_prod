package com.hitendra.turf_booking_backend.entity;

/**
 * Refund lifecycle status.
 * Tracks the state of a refund through Razorpay or wallet.
 */
public enum RefundStatus {
    INITIATED,      // Refund created and pending processing
    PROCESSING,     // Refund is being processed by payment gateway
    SUCCESS,        // Refund completed successfully
    FAILED,         // Refund failed
    CANCELLED       // Refund was cancelled
}
