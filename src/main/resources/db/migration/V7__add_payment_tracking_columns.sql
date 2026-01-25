-- V7: Add payment tracking columns for partial online payment feature
-- Tracks online amount paid and venue amount due

-- Add venue_amount_due column to track amount to be paid at venue
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS venue_amount_due DECIMAL(19, 2);

-- Add venue_amount_collected flag to track if venue payment was collected
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS venue_amount_collected BOOLEAN DEFAULT FALSE;

-- Add comment for documentation
COMMENT ON COLUMN bookings.online_amount_paid IS 'Amount paid online at booking time (X% of total)';
COMMENT ON COLUMN bookings.venue_amount_due IS 'Amount due to be paid at venue (remaining after online payment)';
COMMENT ON COLUMN bookings.venue_amount_collected IS 'Whether the venue amount has been collected by admin/manager';

-- Backfill existing bookings: If online_amount_paid is set, calculate venue_amount_due
UPDATE bookings
SET venue_amount_due = amount - COALESCE(online_amount_paid, 0)
WHERE venue_amount_due IS NULL AND amount IS NOT NULL;

-- For confirmed bookings without online_amount_paid, assume full amount was paid online (legacy bookings)
UPDATE bookings
SET online_amount_paid = amount,
    venue_amount_due = 0,
    venue_amount_collected = TRUE
WHERE status = 'CONFIRMED'
  AND online_amount_paid IS NULL
  AND amount IS NOT NULL;
