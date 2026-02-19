package com.hitendra.turf_booking_backend.entity;

/**
 * Refund lifecycle status.
 * Tracks the state of a refund through Razorpay or wallet.
 * Status names aligned with Razorpay refund statuses.
 */
public enum RefundStatus {
    INITIATED,      // Refund created and pending processing
    PENDING,        // Refund is pending (Razorpay status: pending)
    PROCESSING,     // Refund is being processed by payment gateway
    PROCESSED,      // Refund has been processed successfully (Razorpay status: processed)
    SUCCESS,        // Refund completed successfully (legacy/alternate success status)
    FAILED,         // Refund failed
    CANCELLED       // Refund was cancelled
}
