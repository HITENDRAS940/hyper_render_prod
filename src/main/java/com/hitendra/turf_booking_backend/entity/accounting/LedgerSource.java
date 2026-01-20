package com.hitendra.turf_booking_backend.entity.accounting;

/**
 * Sources of cash ledger entries.
 */
public enum LedgerSource {
    BOOKING,            // Slot booking revenue
    INVENTORY_SALE,     // Cafe/shop sales
    EXPENSE,            // General expenses (electricity, maintenance, salaries)
    INVENTORY_PURCHASE, // Stock purchases
    REFUND,             // Customer refunds
    ADJUSTMENT          // Manual corrections
}

