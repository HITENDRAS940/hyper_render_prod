package com.hitendra.turf_booking_backend.entity.accounting;

/**
 * Sources of cash ledger entries.
 */
public enum LedgerSource {
    BOOKING,            // Slot booking revenue (venue/cash payments only — recorded immediately)
    EXPENSE,            // General expenses (electricity, maintenance, salaries)
    REFUND,             // Customer refunds
    ADJUSTMENT,         // Manual corrections
    SETTLEMENT          // Online advance settled by manager to admin bank (recorded when manager settles)
}

