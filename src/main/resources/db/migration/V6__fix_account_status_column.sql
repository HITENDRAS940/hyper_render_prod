-- V6__fix_account_status_column.sql
-- Fix: Add account_status and deleted_at columns if they don't exist
-- Uses DO block for PostgreSQL compatibility

DO $$
BEGIN
    -- Add account_status column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'account_status'
    ) THEN
        ALTER TABLE users ADD COLUMN account_status VARCHAR(20) DEFAULT 'ACTIVE';
    END IF;

    -- Add deleted_at column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'deleted_at'
    ) THEN
        ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;
    END IF;
END $$;

-- Create indexes (IF NOT EXISTS is supported for indexes)
CREATE INDEX IF NOT EXISTS idx_users_account_status ON users(account_status);
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at);
