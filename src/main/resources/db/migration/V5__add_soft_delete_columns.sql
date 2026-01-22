-- GDPR/Privacy Policy Compliant Account Deletion Schema
-- Adds columns to support permanent account deletion while preserving booking records

-- Add account_status column for tracking account lifecycle
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status VARCHAR(20) DEFAULT 'ACTIVE';

-- Add deleted_at timestamp to track when account was deleted
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Create index for efficient querying of active users
CREATE INDEX IF NOT EXISTS idx_users_account_status ON users(account_status);
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at);

-- Note: When a user requests account deletion:
-- 1. All PII (name, email, phone, oauth_provider, oauth_provider_id) is set to NULL or placeholder
-- 2. account_status is set to 'DELETED'
-- 3. enabled is set to false
-- 4. deleted_at is set to current timestamp
-- 5. All bookings are unlinked (user_id = NULL) but preserved for business records
-- 6. UserProfile is deleted completely
-- 7. For admins: AdminProfile PII is cleared, admin-created bookings are unlinked

