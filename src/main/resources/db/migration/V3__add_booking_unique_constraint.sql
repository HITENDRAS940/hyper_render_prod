-- ═══════════════════════════════════════════════════════════════════════════════════
-- V3: Add partial unique index to prevent double-booking at the database level.
--
-- This ensures that no two active bookings (CONFIRMED, PENDING, or AWAITING_CONFIRMATION)
-- can exist for the same resource + date + start_time combination.
--
-- This is the final safety net for concurrent booking scenarios where two transactions
-- both see the resource as available and try to insert at the same time.
-- ═══════════════════════════════════════════════════════════════════════════════════

CREATE UNIQUE INDEX IF NOT EXISTS idx_booking_no_double_book
    ON bookings (resource_id, booking_date, start_time)
    WHERE status IN ('CONFIRMED', 'PENDING', 'AWAITING_CONFIRMATION');

