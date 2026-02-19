-- ============================================================================
-- Migration: Rename venue_id to service_id
-- Description: Refactor venue references to service for consistency
-- Date: 2026-02-19
-- ============================================================================

-- ============================================================================
-- EXPENSES TABLE: Rename venue_id to service_id
-- ============================================================================

-- Step 1: Check if the old column exists and new column doesn't exist
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'expenses'
        AND column_name = 'venue_id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'expenses'
        AND column_name = 'service_id'
    ) THEN
        -- Rename the column
        ALTER TABLE expenses RENAME COLUMN venue_id TO service_id;

        -- Drop old index if exists
        DROP INDEX IF EXISTS idx_service_expense_date;

        -- Create new index with correct column name
        CREATE INDEX IF NOT EXISTS idx_service_expense_date ON expenses(service_id, expense_date);

        RAISE NOTICE 'Successfully renamed expenses.venue_id to expenses.service_id';
    ELSE
        RAISE NOTICE 'Column expenses.venue_id already renamed or expenses.service_id already exists';
    END IF;
END $$;

-- ============================================================================
-- REFUNDS TABLE: Add service_id column
-- ============================================================================

-- Step 2: Add service_id column to refunds table if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'refunds'
        AND column_name = 'service_id'
    ) THEN
        -- Add the new column
        ALTER TABLE refunds ADD COLUMN service_id BIGINT;

        -- Add foreign key constraint
        ALTER TABLE refunds
        ADD CONSTRAINT fk_refunds_service
        FOREIGN KEY (service_id)
        REFERENCES services(id)
        ON DELETE SET NULL;

        RAISE NOTICE 'Added service_id column to refunds table';
    ELSE
        RAISE NOTICE 'Column refunds.service_id already exists';
    END IF;
END $$;

-- Step 3: Populate service_id from booking relationship
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'refunds'
        AND column_name = 'service_id'
    ) THEN
        -- Update service_id based on the booking's service
        UPDATE refunds r
        SET service_id = b.service_id
        FROM bookings b
        WHERE r.booking_id = b.id
        AND r.service_id IS NULL;

        RAISE NOTICE 'Populated service_id in refunds table from bookings';
    END IF;
END $$;

-- Step 4: Create index for service_id in refunds table
CREATE INDEX IF NOT EXISTS idx_refund_service ON refunds(service_id);

-- ============================================================================
-- VERIFICATION QUERIES (commented out - uncomment to verify)
-- ============================================================================

-- Verify expenses table structure
-- SELECT column_name, data_type, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'expenses'
-- AND column_name IN ('service_id', 'venue_id')
-- ORDER BY column_name;

-- Verify refunds table structure
-- SELECT column_name, data_type, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'refunds'
-- AND column_name = 'service_id';

-- Verify data migration for refunds
-- SELECT
--     COUNT(*) as total_refunds,
--     COUNT(service_id) as refunds_with_service_id,
--     COUNT(*) - COUNT(service_id) as refunds_without_service_id
-- FROM refunds;

-- ============================================================================
-- ROLLBACK SCRIPT (for emergency use only)
-- ============================================================================

-- ROLLBACK STEPS (DO NOT RUN UNLESS NECESSARY):
--
-- -- Rollback expenses table
-- ALTER TABLE expenses RENAME COLUMN service_id TO venue_id;
-- DROP INDEX IF EXISTS idx_service_expense_date;
-- CREATE INDEX idx_service_expense_date ON expenses(venue_id, expense_date);
--
-- -- Rollback refunds table
-- DROP INDEX IF EXISTS idx_refund_service;
-- ALTER TABLE refunds DROP CONSTRAINT IF EXISTS fk_refunds_service;
-- ALTER TABLE refunds DROP COLUMN IF EXISTS service_id;

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================

