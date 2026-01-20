package com.hitendra.turf_booking_backend.entity.accounting;

/**
 * Reference types for ledger entries.
 * Maps to the actual entity being referenced.
 */
public enum ReferenceType {
    BOOKING,            // References Booking entity
    INVENTORY_SALE,     // References InventorySale entity
    EXPENSE,            // References Expense entity
    INVENTORY_PURCHASE, // References InventoryPurchase entity
    REFUND,             // References Refund/Cancellation entity
    ADJUSTMENT          // Manual adjustment (no entity reference)
}

