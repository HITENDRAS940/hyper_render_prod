package com.hitendra.turf_booking_backend.entity;

/**
 * Differentiates the two sub-ledgers of an admin's wallet.
 * CASH  = physical notes collected/spent
 * BANK  = digital/UPI/transfer money in the admin's bank account
 */
public enum AdminLedgerType {
    CASH,
    BANK
}

