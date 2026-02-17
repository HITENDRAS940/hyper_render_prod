package com.hitendra.turf_booking_backend.entity.accounting;

/**
 * Sources of cash ledger entries.
 */
public enum LedgerSource {
    BOOKING,            // Slot booking revenue
    EXPENSE,            // General expenses (electricity, maintenance, salaries)
    REFUND,             // Customer refunds
    ADJUSTMENT          // Manual corrections
}

