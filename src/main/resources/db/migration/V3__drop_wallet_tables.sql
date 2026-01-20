-- V3__drop_wallet_tables.sql
-- Remove wallet-related tables and columns

-- Drop wallet transaction table first (has foreign key to wallets)
DROP TABLE IF EXISTS wallet_transactions CASCADE;

-- Drop wallets table
DROP TABLE IF EXISTS wallets CASCADE;

-- Remove wallet-related columns from bookings table
ALTER TABLE bookings DROP COLUMN IF EXISTS wallet_amount_used;
ALTER TABLE bookings DROP COLUMN IF EXISTS wallet_transaction_id;
ALTER TABLE bookings DROP COLUMN IF EXISTS payment_method_type;
