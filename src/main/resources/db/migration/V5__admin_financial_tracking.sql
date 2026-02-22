-- ============================================================================
-- Migration V5: Admin Financial Tracking
--
-- Introduces per-admin financial ledger fields, a settlements table, and
-- a financial_transactions audit table that covers all three money flows:
--
--   Flow 1 – ADVANCE_ONLINE  : Platform collects → pendingOnlineAmount
--   Flow 2 – VENUE_CASH      : Admin collects cash → cashBalance
--   Flow 3 – VENUE_BANK      : Admin collects online direct → bankBalance
--   Flow 4 – SETTLEMENT      : Manager settles → pendingOnlineAmount→bankBalance
-- ============================================================================

-- ─── 1. Extend admin_profiles with financial columns ─────────────────────────
-- Using ADD COLUMN IF NOT EXISTS so this is safe to re-run after a partial failure.
-- Columns are added as NULLABLE first, then backfilled and constrained — this is
-- the safest pattern for ALTER TABLE on non-empty tables in PostgreSQL.

ALTER TABLE admin_profiles
    ADD COLUMN IF NOT EXISTS total_cash_collected            NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS total_bank_collected            NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS total_platform_online_collected NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS total_settled_amount            NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS pending_online_amount           NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS cash_balance                    NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS bank_balance                    NUMERIC(19, 2);

-- Backfill NULL rows with 0 (safe for existing rows)
UPDATE admin_profiles
SET
    total_cash_collected            = COALESCE(total_cash_collected, 0),
    total_bank_collected            = COALESCE(total_bank_collected, 0),
    total_platform_online_collected = COALESCE(total_platform_online_collected, 0),
    total_settled_amount            = COALESCE(total_settled_amount, 0),
    pending_online_amount           = COALESCE(pending_online_amount, 0),
    cash_balance                    = COALESCE(cash_balance, 0),
    bank_balance                    = COALESCE(bank_balance, 0);

-- Now apply NOT NULL + DEFAULT constraints
ALTER TABLE admin_profiles
    ALTER COLUMN total_cash_collected            SET NOT NULL,
    ALTER COLUMN total_cash_collected            SET DEFAULT 0,
    ALTER COLUMN total_bank_collected            SET NOT NULL,
    ALTER COLUMN total_bank_collected            SET DEFAULT 0,
    ALTER COLUMN total_platform_online_collected SET NOT NULL,
    ALTER COLUMN total_platform_online_collected SET DEFAULT 0,
    ALTER COLUMN total_settled_amount            SET NOT NULL,
    ALTER COLUMN total_settled_amount            SET DEFAULT 0,
    ALTER COLUMN pending_online_amount           SET NOT NULL,
    ALTER COLUMN pending_online_amount           SET DEFAULT 0,
    ALTER COLUMN cash_balance                    SET NOT NULL,
    ALTER COLUMN cash_balance                    SET DEFAULT 0,
    ALTER COLUMN bank_balance                    SET NOT NULL,
    ALTER COLUMN bank_balance                    SET DEFAULT 0;

-- ─── 2. Settlements table ─────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS settlements (
    id                      BIGSERIAL PRIMARY KEY,

    -- The admin being settled
    admin_id                BIGINT NOT NULL REFERENCES admin_profiles(id),

    -- Amount transferred
    amount                  NUMERIC(19, 2) NOT NULL,

    -- Transfer method used by the manager
    payment_mode            VARCHAR(50)  NOT NULL,   -- BANK_TRANSFER | UPI

    -- Lifecycle status
    status                  VARCHAR(50)  NOT NULL DEFAULT 'INITIATED',  -- INITIATED | SUCCESS | FAILED

    -- Manager who initiated this settlement
    settled_by_manager_id   BIGINT,

    -- External reference (UTR number, transaction ID, etc.)
    settlement_reference    VARCHAR(255),

    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_settlement_admin     ON settlements (admin_id);
CREATE INDEX IF NOT EXISTS idx_settlement_created   ON settlements (created_at);

-- ─── 3. Financial transactions audit table ────────────────────────────────────

CREATE TABLE IF NOT EXISTS financial_transactions (
    id              BIGSERIAL PRIMARY KEY,

    -- Admin this transaction belongs to
    admin_id        BIGINT NOT NULL REFERENCES admin_profiles(id),

    -- Transaction type:
    --   ADVANCE_ONLINE  → bookingId
    --   VENUE_CASH      → bookingId
    --   VENUE_BANK      → bookingId
    --   SETTLEMENT      → settlementId
    type            VARCHAR(50)  NOT NULL,

    -- Positive amount (credits/debits determined by type)
    amount          NUMERIC(19, 2) NOT NULL,

    -- Contextual reference ID (bookingId or settlementId)
    reference_id    BIGINT,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fin_tx_admin      ON financial_transactions (admin_id);
CREATE INDEX IF NOT EXISTS idx_fin_tx_type_ref   ON financial_transactions (type, reference_id);
CREATE INDEX IF NOT EXISTS idx_fin_tx_created    ON financial_transactions (created_at);

-- ============================================================================
-- END OF MIGRATION V5
-- ============================================================================


CREATE TABLE IF NOT EXISTS settlements (
    id                      BIGSERIAL PRIMARY KEY,

    -- The admin being settled
    admin_id                BIGINT NOT NULL REFERENCES admin_profiles(id),

    -- Amount transferred
    amount                  NUMERIC(19, 2) NOT NULL,

    -- Transfer method used by the manager
    payment_mode            VARCHAR(50)  NOT NULL,   -- BANK_TRANSFER | UPI

    -- Lifecycle status
    status                  VARCHAR(50)  NOT NULL DEFAULT 'INITIATED',  -- INITIATED | SUCCESS | FAILED

    -- Manager who initiated this settlement
    settled_by_manager_id   BIGINT,

    -- External reference (UTR number, transaction ID, etc.)
    settlement_reference    VARCHAR(255),

    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_settlement_admin     ON settlements (admin_id);
CREATE INDEX IF NOT EXISTS idx_settlement_created   ON settlements (created_at);

-- ─── 3. Financial transactions audit table ────────────────────────────────────

CREATE TABLE IF NOT EXISTS financial_transactions (
    id              BIGSERIAL PRIMARY KEY,

    -- Admin this transaction belongs to
    admin_id        BIGINT NOT NULL REFERENCES admin_profiles(id),

    -- Transaction type:
    --   ADVANCE_ONLINE  → bookingId
    --   VENUE_CASH      → bookingId
    --   VENUE_BANK      → bookingId
    --   SETTLEMENT      → settlementId
    type            VARCHAR(50)  NOT NULL,

    -- Positive amount (credits/debits determined by type)
    amount          NUMERIC(19, 2) NOT NULL,

    -- Contextual reference ID (bookingId or settlementId)
    reference_id    BIGINT,

    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fin_tx_admin      ON financial_transactions (admin_id);
CREATE INDEX IF NOT EXISTS idx_fin_tx_type_ref   ON financial_transactions (type, reference_id);
CREATE INDEX IF NOT EXISTS idx_fin_tx_created    ON financial_transactions (created_at);

-- ============================================================================
-- END OF MIGRATION V5
-- ============================================================================


