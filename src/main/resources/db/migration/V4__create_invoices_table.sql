-- ============================================================================
-- Migration V4: Create invoices table
-- Stores invoice records received from the invoice generator service.
-- ============================================================================

CREATE TABLE IF NOT EXISTS invoices (
    id                          BIGSERIAL PRIMARY KEY,

    -- Link to the booking (one-to-one)
    booking_id                  BIGINT NOT NULL UNIQUE REFERENCES bookings(id),

    -- Invoice document URL
    invoice_url                 VARCHAR(1024) NOT NULL,

    -- Booking snapshot (captured at invoice receipt time)
    booking_reference           VARCHAR(255),
    booking_date                DATE,
    start_time                  TIME,
    end_time                    TIME,
    total_amount                DOUBLE PRECISION,
    online_amount_paid          NUMERIC(19, 2),
    venue_amount_due            NUMERIC(19, 2),

    -- Payment details
    payment_method              VARCHAR(100),
    payment_mode                VARCHAR(100),
    razorpay_payment_id         VARCHAR(255),

    -- Service snapshot
    service_id                  BIGINT,
    service_name                VARCHAR(255),

    -- Resource snapshot
    resource_id                 BIGINT,
    resource_name               VARCHAR(255),

    -- User snapshot
    user_id                     BIGINT,
    user_name                   VARCHAR(255),
    user_email                  VARCHAR(255),
    user_phone                  VARCHAR(50),

    -- Audit
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_invoice_booking_id ON invoices (booking_id);

-- ============================================================================
-- END OF MIGRATION
-- ============================================================================

