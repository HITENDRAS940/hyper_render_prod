package com.hitendra.turf_booking_backend.entity.accounting;

/**
 * Payment modes for all financial transactions.
 */
public enum PaymentMode {
    CASH,
    UPI,
    ONLINE,         // Generic online payment (UPI/Card/NetBanking at venue)
    BANK_TRANSFER,
    CARD,
    CHEQUE
}

