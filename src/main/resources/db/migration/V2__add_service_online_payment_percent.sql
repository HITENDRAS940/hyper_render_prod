-- ============================================================================
-- V2__add_service_online_payment_percent_and_coupons.sql
-- ============================================================================
-- Combines two sets of changes applied after the V1 baseline:
--
--   A. Per-service online payment percentage (services table)
--      Previously the percentage of the total booking amount the customer
--      must pay online was a global application property only.
--      This adds a per-service override column with NULL = global fallback.
--
--   B. Full coupon system
--      1. coupons                      – coupon definitions with rich constraints
--      2. coupon_applicable_services   – service-scope whitelist
--      3. coupon_applicable_resources  – resource-scope whitelist
--      4. coupon_applicable_activities – activity-scope whitelist
--      5. coupon_usages                – per-booking redemption log
--      6. bookings columns             – discount_amount + applied_coupon_code
--
-- PostgreSQL-compatible. All DDL is idempotent (IF NOT EXISTS / ADD COLUMN IF NOT EXISTS).
-- ============================================================================

-- ════════════════════════════════════════════════════════════════════════════
-- A. services — per-service online payment percentage
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE services
    ADD COLUMN IF NOT EXISTS online_payment_percent DOUBLE PRECISION DEFAULT NULL;

COMMENT ON COLUMN services.online_payment_percent IS
    'Per-service online payment percentage (0-100). '
    'NULL means use the global pricing.online-payment-percent application config as fallback.';

-- ════════════════════════════════════════════════════════════════════════════
-- B. Coupon system
-- ════════════════════════════════════════════════════════════════════════════

-- ── 1. coupons ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS coupons (
    id                           BIGSERIAL PRIMARY KEY,

    -- Identity
    code                         VARCHAR(255)     NOT NULL,
    description                  VARCHAR(500),

    -- Discount
    discount_type                VARCHAR(50)      NOT NULL,  -- PERCENTAGE | FIXED
    discount_value               DOUBLE PRECISION NOT NULL,
    min_booking_amount           DOUBLE PRECISION,
    max_discount_amount          DOUBLE PRECISION,           -- cap for PERCENTAGE type

    -- Validity window
    valid_from                   DATE,                       -- NULL = immediately active
    expiry_date                  DATE             NOT NULL,

    -- Usage limits
    active                       BOOLEAN          NOT NULL DEFAULT TRUE,
    usage_limit                  INTEGER,                    -- NULL = unlimited global
    current_usage                INTEGER          NOT NULL DEFAULT 0,
    per_user_usage_limit         INTEGER          NOT NULL DEFAULT 1,

    -- User constraints
    new_users_only               BOOLEAN          NOT NULL DEFAULT FALSE,

    -- Booking constraints
    min_booking_duration_minutes INTEGER,                    -- NULL = no restriction
    valid_day_type               VARCHAR(20),                -- WEEKDAY | WEEKEND | ALL | NULL = ALL

    created_at                   TIMESTAMPTZ      NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_coupon_code UNIQUE (code)
);

CREATE INDEX IF NOT EXISTS idx_coupon_code ON coupons (code);

-- ── 2. coupon_applicable_services ────────────────────────────────────────────
-- If rows exist for a coupon it is restricted to those service IDs only.
-- Empty = valid for ALL services.
CREATE TABLE IF NOT EXISTS coupon_applicable_services (
    coupon_id  BIGINT NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    service_id BIGINT NOT NULL,
    PRIMARY KEY (coupon_id, service_id)
);

-- ── 3. coupon_applicable_resources ───────────────────────────────────────────
-- If rows exist for a coupon it is restricted to those resource IDs only.
CREATE TABLE IF NOT EXISTS coupon_applicable_resources (
    coupon_id   BIGINT NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    resource_id BIGINT NOT NULL,
    PRIMARY KEY (coupon_id, resource_id)
);

-- ── 4. coupon_applicable_activities ──────────────────────────────────────────
-- If rows exist for a coupon it is restricted to those activity codes only.
CREATE TABLE IF NOT EXISTS coupon_applicable_activities (
    coupon_id     BIGINT      NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    activity_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (coupon_id, activity_code)
);

-- ── 5. coupon_usages ─────────────────────────────────────────────────────────
-- One row per booking redemption. No unique constraint on (user, coupon) so
-- multi-use coupons (per_user_usage_limit > 1) are supported; the per-user
-- limit is enforced in application code via countByUserIdAndCouponId.
CREATE TABLE IF NOT EXISTS coupon_usages (
    id         BIGSERIAL   PRIMARY KEY,
    coupon_id  BIGINT      NOT NULL REFERENCES coupons(id)   ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
    booking_id BIGINT      NOT NULL REFERENCES bookings(id)  ON DELETE CASCADE,
    used_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_coupon_usage_user_coupon ON coupon_usages (user_id, coupon_id);

-- ── 6. bookings – discount columns ───────────────────────────────────────────
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS discount_amount      NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS applied_coupon_code  VARCHAR(255);
