-- V16: Add venue payment collection method column
-- Tracks how the venue amount was collected (CASH or ONLINE)

-- Add venue_payment_collection_method column to track collection method
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS venue_payment_collection_method VARCHAR(20);

-- Add comment for documentation
COMMENT ON COLUMN bookings.venue_payment_collection_method IS 'Method used to collect venue amount: CASH or ONLINE';

