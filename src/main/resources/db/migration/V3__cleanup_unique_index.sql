-- ============================================================================
-- Migration V3: Cleanup - Remove the partial unique index that was
-- incorrectly applied by a previous failed migration attempt.
--
-- Background:
--   A prior migration attempt created a partial unique index
--   "idx_booking_no_double_book" on bookings(resource_id, booking_date, start_time)
--   and incorrectly cancelled duplicate active bookings to allow the index creation.
--
--   This migration:
--     1. Drops that index so the booking table behaves as before.
--     2. Restores bookings that were force-cancelled by the prior migration
--        back to PENDING so they can follow their natural expiry/payment flow.
-- ============================================================================

-- Step 1: Drop the partial unique index if it exists.
DROP INDEX IF EXISTS idx_booking_no_double_book;

-- Step 2: Restore bookings that were incorrectly force-cancelled.
--   We identify them as bookings that:
--     - Are currently CANCELLED
--     - Have a reference (were real bookings, not test data)
--     - Have a payment_status of NOT_STARTED (never paid — the ones
--       the old migration targeted as "older duplicates")
--     - Were created today (2026-02-19), the date the bad migration ran
--   We restore them to PENDING so the normal expiry scheduler can clean
--   them up gracefully, or the user can retry payment.
UPDATE bookings
SET status = 'PENDING'
WHERE status = 'CANCELLED'
  AND payment_status = 'NOT_STARTED'
  AND reference IS NOT NULL
  AND DATE(created_at AT TIME ZONE 'UTC') = '2026-02-19'
  AND id NOT IN (
      -- Do NOT restore if a CONFIRMED/PENDING booking already exists
      -- for the same resource+date+start_time (the "winner" booking).
      -- We only restore the ones that don't conflict with an active booking.
      SELECT b2.id
      FROM bookings b2
      WHERE EXISTS (
          SELECT 1 FROM bookings b3
          WHERE b3.resource_id  = b2.resource_id
            AND b3.booking_date = b2.booking_date
            AND b3.start_time   = b2.start_time
            AND b3.status IN ('CONFIRMED', 'PENDING', 'AWAITING_CONFIRMATION')
            AND b3.id != b2.id
      )
      AND b2.status = 'CANCELLED'
      AND b2.payment_status = 'NOT_STARTED'
  );

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================

