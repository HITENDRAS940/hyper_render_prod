-- V15: Add financial ledger tables and columns

-- Add columns to bookings table
ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS advance_amount DECIMAL(19, 2),
ADD COLUMN IF NOT EXISTS remaining_amount DECIMAL(19, 2),
ADD COLUMN IF NOT EXISTS transfer_status VARCHAR(20) DEFAULT 'PENDING';

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_booking_service_date ON bookings(service_id, booking_date);
CREATE INDEX IF NOT EXISTS idx_booking_service_payment ON bookings(service_id, payment_status);

-- Update existing bookings to have consistent data (optional, but good practice)
-- Assuming existing online_amount_paid is the advance_amount and venue_amount_due is remaining_amount
UPDATE bookings
SET advance_amount = online_amount_paid,
    remaining_amount = venue_amount_due
WHERE advance_amount IS NULL;

-- Create expenses table
CREATE TABLE IF NOT EXISTS expenses (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL,
    category VARCHAR(100) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    payment_mode VARCHAR(20) CHECK (payment_mode IN ('CASH', 'BANK')),
    description TEXT,
    expense_date DATE NOT NULL,
    bill_url TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for expenses
CREATE INDEX IF NOT EXISTS idx_expense_service_date ON expenses(service_id, expense_date);

-- Add service_id to refunds table for optimized reporting
ALTER TABLE refunds
ADD COLUMN IF NOT EXISTS service_id BIGINT;

-- Populate service_id from bookings
UPDATE refunds r
SET service_id = b.service_id
FROM bookings b
WHERE b.id = r.booking_id AND r.service_id IS NULL;

-- Index for refunds
CREATE INDEX IF NOT EXISTS idx_refund_service_date ON refunds(service_id, initiated_at);



