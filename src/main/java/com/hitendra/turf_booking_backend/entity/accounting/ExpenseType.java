package com.hitendra.turf_booking_backend.entity.accounting;

/**
 * Expense types for categorization.
 * FIXED: Regular, predictable expenses (salaries, rent, etc.)
 * VARIABLE: Fluctuating expenses (maintenance, inventory, etc.)
 */
public enum ExpenseType {
    FIXED,      // Salaries, Rent, Subscriptions
    VARIABLE    // Maintenance, Inventory, Utilities (varies)
}

