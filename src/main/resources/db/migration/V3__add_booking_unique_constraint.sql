-- ═══════════════════════════════════════════════════════════════════════════════════
-- V3: Add partial unique index to prevent double-booking at the database level.
--
-- The index only covers TRULY LOCKED bookings:
--   - CONFIRMED (payment done, slot is definitively taken)
--   - PENDING/AWAITING_CONFIRMATION where payment is IN_PROGRESS or SUCCESS
--     (user is actively paying right now)
--
-- This intentionally EXCLUDES PENDING bookings where payment_status = 'NOT_STARTED'.
-- Those are "soft" holds — the user selected a slot but hasn't initiated payment yet.
-- Another user should be able to get a different resource for the same slot,
-- and if the first user abandons, the booking expires automatically.
--
-- STEP 1: Clean up any pre-existing duplicates that would block index creation.
--         Keep the most recent booking (highest id) per locked group, cancel older ones.
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
        WHERE (
            status = 'CONFIRMED'
            OR (
                status IN ('PENDING', 'AWAITING_CONFIRMATION')
                AND payment_status IN ('IN_PROGRESS', 'SUCCESS')
            )
        )
    ) ranked
    WHERE rn > 1
);

-- STEP 2: Create the partial unique index — only for truly locked bookings.
CREATE UNIQUE INDEX IF NOT EXISTS idx_booking_no_double_book
    ON bookings (resource_id, booking_date, start_time)
    WHERE (
        status = 'CONFIRMED'
        OR (
            status IN ('PENDING', 'AWAITING_CONFIRMATION')
            AND payment_status IN ('IN_PROGRESS', 'SUCCESS')
        )
    );


