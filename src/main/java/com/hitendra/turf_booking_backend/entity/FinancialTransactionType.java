package com.hitendra.turf_booking_backend.entity;

public enum FinancialTransactionType {
    ADVANCE_ONLINE,   // Online advance paid at booking time (platform collects)
    VENUE_CASH,       // Remaining paid at venue in cash (admin collects directly)
    VENUE_BANK,       // Remaining paid at venue via online/UPI direct to admin bank
    SETTLEMENT        // Manager settles platform-collected funds to admin
}

