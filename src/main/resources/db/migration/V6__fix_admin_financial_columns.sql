-- ============================================================================
-- Migration V6: Fix admin_profiles financial columns NOT NULL + DEFAULT
--
-- V5 adds the columns as NULLABLE first, then sets NOT NULL + DEFAULT.
-- On some databases (e.g. Render staging) the ALTER TABLE in V5 was recorded
-- as SUCCESS before the constraints were actually applied.
-- This migration re-applies ONLY the NOT NULL + DEFAULT constraints
-- idempotently (SET NOT NULL / SET DEFAULT are always safe to re-run).
-- The settlements and financial_transactions tables already exist from V5.
-- ============================================================================

-- ─── Re-apply NOT NULL + DEFAULT on admin_profiles financial columns ──────────

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


-- ============================================================================
-- END OF MIGRATION V6
-- ============================================================================

