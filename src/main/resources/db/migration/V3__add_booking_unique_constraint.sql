-- ═══════════════════════════════════════════════════════════════════════════════════
-- V3: Add partial unique index to prevent double-booking at the database level.
--
-- This ensures that no two active bookings (CONFIRMED, PENDING, or AWAITING_CONFIRMATION)
-- can exist for the same resource + date + start_time combination.
--
-- This is the final safety net for concurrent booking scenarios where two transactions
-- both see the resource as available and try to insert at the same time.
--
-- STEP 1: Clean up any pre-existing duplicate active bookings.
--         For each duplicate group, keep the one with the highest (most recent) id
--         and cancel all older duplicates to allow the unique index to be created.
-- ═══════════════════════════════════════════════════════════════════════════════════

UPDATE bookings
SET status = 'CANCELLED'
WHERE id IN (
    SELECT id
    FROM (
        SELECT
            id,
            ROW_NUMBER() OVER (
                PARTITION BY resource_id, booking_date, start_time
                ORDER BY id DESC
            ) AS rn
        FROM bookings
        WHERE status IN ('CONFIRMED', 'PENDING', 'AWAITING_CONFIRMATION')
    ) ranked
    WHERE rn > 1
);

-- STEP 2: Create the partial unique index now that duplicates are resolved.
CREATE UNIQUE INDEX IF NOT EXISTS idx_booking_no_double_book
    ON bookings (resource_id, booking_date, start_time)
    WHERE status IN ('CONFIRMED', 'PENDING', 'AWAITING_CONFIRMATION');

