-- Add GSTIN and state fields to services table for tax invoice generation
ALTER TABLE services ADD COLUMN IF NOT EXISTS gstin VARCHAR(15);
ALTER TABLE services ADD COLUMN IF NOT EXISTS state VARCHAR(100);

-- Add address and GSTIN fields to users table for tax invoice generation
ALTER TABLE users ADD COLUMN IF NOT EXISTS address TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS gstin VARCHAR(15);

