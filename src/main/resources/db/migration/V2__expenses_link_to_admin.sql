-- ============================================================================
-- V2__expenses_link_to_admin.sql
-- ============================================================================
-- Migrates the `expenses` table from being linked to a service to being
-- linked directly to an admin profile.
--
-- Changes:
--   1. Add `admin_profile_id` column (resolved from the service owner)
--   2. Populate it for all existing rows
--   3. Make it NOT NULL
--   4. Drop the old `service_id` FK column and its index
--   5. Create the new index on admin_profile_id + expense_date
-- ============================================================================

-- Step 1: Add nullable column first so existing rows don't violate NOT NULL
ALTER TABLE expenses
    ADD COLUMN IF NOT EXISTS admin_profile_id BIGINT;

-- Step 2: Backfill — resolve admin_profile_id from the service owner
UPDATE expenses e
SET admin_profile_id = (
    SELECT s.created_by_admin_id
    FROM services s
    WHERE s.id = e.service_id
)
WHERE e.service_id IS NOT NULL;

-- Step 3: For any orphaned rows (service deleted), assign NULL stays—
--         we enforce NOT NULL only after the update so nothing is skipped.
--         If you want to hard-delete orphaned rows instead, uncomment below:
-- DELETE FROM expenses WHERE admin_profile_id IS NULL;

-- Step 4: Set NOT NULL constraint now that data is populated
ALTER TABLE expenses
    ALTER COLUMN admin_profile_id SET NOT NULL;

-- Step 5: Add the FK constraint
ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_admin_profile
        FOREIGN KEY (admin_profile_id)
            REFERENCES admin_profiles(id)
            ON DELETE CASCADE;

-- Step 6: Drop the old index and FK on service_id
DROP INDEX IF EXISTS idx_service_expense_date;

ALTER TABLE expenses
    DROP CONSTRAINT IF EXISTS expenses_service_id_fkey;

-- Step 7: Drop the old column
ALTER TABLE expenses
    DROP COLUMN IF EXISTS service_id;

-- Step 8: Create new index
CREATE INDEX IF NOT EXISTS idx_admin_expense_date ON expenses(admin_profile_id, expense_date);

