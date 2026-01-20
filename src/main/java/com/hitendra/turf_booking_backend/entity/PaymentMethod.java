package com.hitendra.turf_booking_backend.entity;

/**
 * Payment method options for booking.
 */
public enum PaymentMethod {
    WALLET_ONLY,        // Pay entirely from wallet
    ONLINE_ONLY,        // Pay entirely via online payment
    WALLET_PLUS_ONLINE  // Partial wallet + partial online
}

