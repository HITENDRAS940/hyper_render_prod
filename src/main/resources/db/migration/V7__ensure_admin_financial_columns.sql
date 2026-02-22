-- ============================================================================
-- Migration V7: Ensure admin_profiles financial columns exist
--
-- V5 and V6 may have been recorded as SUCCESS in flyway_schema_history on
-- some databases before the ALTER TABLE statements actually executed.
-- This migration is a safe, idempotent catch-up that guarantees all
-- financial columns exist regardless of previous migration state.
-- ============================================================================

-- ─── 1. Add any missing financial columns to admin_profiles ──────────────────

ALTER TABLE admin_profiles
    ADD COLUMN IF NOT EXISTS total_cash_collected            NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS total_bank_collected            NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS total_platform_online_collected NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS total_settled_amount            NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS pending_online_amount           NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS cash_balance                    NUMERIC(19, 2),
    ADD COLUMN IF NOT EXISTS bank_balance                    NUMERIC(19, 2);

-- ─── 2. Backfill any NULLs to 0 ──────────────────────────────────────────────

UPDATE admin_profiles
SET
    total_cash_collected            = COALESCE(total_cash_collected, 0),
    total_bank_collected            = COALESCE(total_bank_collected, 0),
    total_platform_online_collected = COALESCE(total_platform_online_collected, 0),
    total_settled_amount            = COALESCE(total_settled_amount, 0),
    pending_online_amount           = COALESCE(pending_online_amount, 0),
    cash_balance                    = COALESCE(cash_balance, 0),
    bank_balance                    = COALESCE(bank_balance, 0);

-- ─── 3. Apply NOT NULL + DEFAULT constraints (safe even if already set) ───────

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

-- ─── 4. Ensure settlements table exists ───────────────────────────────────────

CREATE TABLE IF NOT EXISTS settlements (
    id                      BIGSERIAL PRIMARY KEY,
    admin_id                BIGINT NOT NULL REFERENCES admin_profiles(id),
    amount                  NUMERIC(19, 2) NOT NULL,
    payment_mode            VARCHAR(50)  NOT NULL,
    status                  VARCHAR(50)  NOT NULL DEFAULT 'INITIATED',
    settled_by_manager_id   BIGINT,
    settlement_reference    VARCHAR(255),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_settlement_admin   ON settlements (admin_id);
CREATE INDEX IF NOT EXISTS idx_settlement_created ON settlements (created_at);

-- ─── 5. Ensure financial_transactions table exists ────────────────────────────

CREATE TABLE IF NOT EXISTS financial_transactions (
    id              BIGSERIAL PRIMARY KEY,
    admin_id        BIGINT NOT NULL REFERENCES admin_profiles(id),
    type            VARCHAR(50)  NOT NULL,
    amount          NUMERIC(19, 2) NOT NULL,
    reference_id    BIGINT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fin_tx_admin    ON financial_transactions (admin_id);
CREATE INDEX IF NOT EXISTS idx_fin_tx_type_ref ON financial_transactions (type, reference_id);
CREATE INDEX IF NOT EXISTS idx_fin_tx_created  ON financial_transactions (created_at);

-- ============================================================================
-- END OF MIGRATION V7
-- ============================================================================

