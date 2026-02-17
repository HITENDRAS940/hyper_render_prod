package com.hitendra.turf_booking_backend.entity.accounting;

/**
 * Reference types for ledger entries.
 * Maps to the actual entity being referenced.
 */
public enum ReferenceType {
    BOOKING,            // References Booking entity
    EXPENSE,            // References Expense entity
    REFUND,             // References Refund/Cancellation entity
    ADJUSTMENT          // Manual adjustment (no entity reference)
}

