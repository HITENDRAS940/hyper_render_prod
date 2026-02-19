-- ============================================================================
-- Migration V3: Drop the partial unique index that was incorrectly created
-- by a prior failed migration, which is now blocking new bookings.
-- ============================================================================

DROP INDEX IF EXISTS idx_booking_no_double_book;

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================

