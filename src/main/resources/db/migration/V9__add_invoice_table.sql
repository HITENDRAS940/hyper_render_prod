-- V9__add_invoice_table.sql
-- Create invoice table for storing booking invoices

CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL UNIQUE,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    cloudinary_url VARCHAR(500) NOT NULL,
    cloudinary_public_id VARCHAR(255),
    invoice_amount DECIMAL(19, 2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

-- Create index on booking_id for fast lookups
CREATE UNIQUE INDEX IF NOT EXISTS idx_invoice_booking_id ON invoices(booking_id);

-- Create index on invoice_number for fast lookups
CREATE INDEX IF NOT EXISTS idx_invoice_number ON invoices(invoice_number);

-- Add comment
COMMENT ON TABLE invoices IS 'Stores invoice metadata and Cloudinary URLs for confirmed bookings';
COMMENT ON COLUMN invoices.booking_id IS 'Foreign key to bookings table (one-to-one relationship)';
COMMENT ON COLUMN invoices.invoice_number IS 'Invoice number in format INV-{bookingId}';
COMMENT ON COLUMN invoices.cloudinary_url IS 'Cloudinary secure URL for the PDF invoice';
COMMENT ON COLUMN invoices.cloudinary_public_id IS 'Cloudinary public ID for deletion if needed';

