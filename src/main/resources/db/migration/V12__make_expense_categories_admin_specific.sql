-- V12: Make expense categories admin-specific
-- Add admin_profile_id to expense_categories table

-- Add admin_profile_id column to expense_categories
ALTER TABLE expense_categories
ADD COLUMN admin_profile_id BIGINT;

-- Add foreign key constraint
ALTER TABLE expense_categories
ADD CONSTRAINT fk_expense_category_admin_profile
FOREIGN KEY (admin_profile_id) REFERENCES admin_profiles(id) ON DELETE CASCADE;

-- Add index for performance
CREATE INDEX idx_expense_category_admin ON expense_categories(admin_profile_id);

-- Update existing categories to be owned by first admin (if any exist)
-- This ensures existing data remains valid
UPDATE expense_categories
SET admin_profile_id = (SELECT id FROM admin_profiles ORDER BY id LIMIT 1)
WHERE admin_profile_id IS NULL
AND EXISTS (SELECT 1 FROM admin_profiles);

-- Make admin_profile_id nullable for now to support migration
-- Future categories will require admin_profile_id when created
-- Note: We keep it nullable to support the transition, but application logic will enforce it

-- Remove unique constraint on name since each admin can have their own "Electricity" category
ALTER TABLE expense_categories
DROP CONSTRAINT IF EXISTS expense_categories_name_key;

-- Add composite unique constraint: name must be unique per admin
ALTER TABLE expense_categories
ADD CONSTRAINT uk_expense_category_admin_name
UNIQUE (admin_profile_id, name);

COMMENT ON COLUMN expense_categories.admin_profile_id IS 'Admin who owns this expense category. Each admin has their own categories.';

