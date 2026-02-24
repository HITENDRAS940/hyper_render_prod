package com.hitendra.turf_booking_backend.entity.accounting;

/**
 * Payment modes allowed for expenses.
 * Expenses can only be paid from CASH (admin's cash balance)
 * or BANK (admin's bank/UPI balance).
 */
public enum ExpensePaymentMode {
    CASH,
    BANK
}

