-- ============================================================================
-- V3__upgrade_coupons_table.sql
-- ============================================================================
-- The coupons table was initially created from V1 with a minimal schema
-- (missing the enhanced constraint columns added later).
-- This migration adds ALL missing columns idempotently so the live DB
-- matches the full Coupon entity definition.
--
-- Missing columns being added:
--   - description
--   - valid_from
--   - per_user_usage_limit
--   - new_users_only
--   - min_booking_duration_minutes
--   - valid_day_type
--
-- Also adds the scope whitelist tables and fixes coupon_usages:
--   - coupon_applicable_services
--   - coupon_applicable_resources
--   - coupon_applicable_activities
--   - removes old UNIQUE constraint on coupon_usages (user_id, coupon_id)
--     to support multi-use coupons (per_user_usage_limit > 1)
--   - adds performance index on coupon_usages (user_id, coupon_id)
--
-- PostgreSQL-compatible. All DDL is idempotent (ADD COLUMN IF NOT EXISTS /
-- CREATE TABLE IF NOT EXISTS / CREATE INDEX IF NOT EXISTS).
-- ============================================================================

-- ── 1. Add missing columns to coupons ────────────────────────────────────────

ALTER TABLE coupons
    ADD COLUMN IF NOT EXISTS description                  VARCHAR(500),
    ADD COLUMN IF NOT EXISTS valid_from                   DATE,
    ADD COLUMN IF NOT EXISTS per_user_usage_limit         INTEGER          NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS new_users_only               BOOLEAN          NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS min_booking_duration_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS valid_day_type               VARCHAR(20);

-- ── 2. Scope whitelist tables (safe to re-run) ────────────────────────────────

CREATE TABLE IF NOT EXISTS coupon_applicable_services (
    coupon_id  BIGINT NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    service_id BIGINT NOT NULL,
    PRIMARY KEY (coupon_id, service_id)
);

CREATE TABLE IF NOT EXISTS coupon_applicable_resources (
    coupon_id   BIGINT NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    resource_id BIGINT NOT NULL,
    PRIMARY KEY (coupon_id, resource_id)
);

CREATE TABLE IF NOT EXISTS coupon_applicable_activities (
    coupon_id     BIGINT      NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    activity_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (coupon_id, activity_code)
);

-- ── 3. coupon_usages — drop old unique constraint, add index ──────────────────
-- The old unique constraint on (user_id, coupon_id) prevents multi-use coupons.
-- Drop it if it still exists; enforcement is now done in application code.

ALTER TABLE coupon_usages
    DROP CONSTRAINT IF EXISTS uk_user_coupon;

CREATE INDEX IF NOT EXISTS idx_coupon_usage_user_coupon
    ON coupon_usages (user_id, coupon_id);

-- ── 4. bookings — discount columns (idempotent) ───────────────────────────────

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS discount_amount      NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS applied_coupon_code  VARCHAR(255);

