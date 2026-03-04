-- ============================================================================
-- V2__post_init_changes.sql
-- ============================================================================
-- Consolidates ALL schema changes made after the V1 baseline:
--
--   A. services — per-service online payment percentage
--   B. Coupon system (full schema)
--        coupons, coupon_applicable_services, coupon_applicable_resources,
--        coupon_applicable_activities, coupon_usages
--   C. bookings — discount_amount + applied_coupon_code columns
--   D. services — resource_selection_mode column
--
-- PostgreSQL-compatible. Fully idempotent (ADD COLUMN IF NOT EXISTS /
-- CREATE TABLE IF NOT EXISTS / CREATE INDEX IF NOT EXISTS).
-- Safe to run on a database that already had the old V2 / V3 / V4 applied.
-- ============================================================================

-- ════════════════════════════════════════════════════════════════════════════
-- A. services — per-service online payment percentage
-- ════════════════════════════════════════════════════════════════════════════
-- NULL = fall back to the global pricing.online-payment-percent app config.

ALTER TABLE services
    ADD COLUMN IF NOT EXISTS online_payment_percent DOUBLE PRECISION DEFAULT NULL;

COMMENT ON COLUMN services.online_payment_percent IS
    'Per-service online payment percentage (0–100). '
    'NULL = use global pricing.online-payment-percent config as fallback.';

-- ════════════════════════════════════════════════════════════════════════════
-- B. Coupon system
-- ════════════════════════════════════════════════════════════════════════════

-- ── B1. coupons ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS coupons (
    id                           BIGSERIAL        PRIMARY KEY,

    -- Identity
    code                         VARCHAR(255)     NOT NULL,
    description                  VARCHAR(500),

    -- Discount
    discount_type                VARCHAR(50)      NOT NULL,   -- PERCENTAGE | FIXED
    discount_value               DOUBLE PRECISION NOT NULL,
    min_booking_amount           DOUBLE PRECISION,
    max_discount_amount          DOUBLE PRECISION,            -- cap for PERCENTAGE type

    -- Validity window
    valid_from                   DATE,                        -- NULL = immediately active
    expiry_date                  DATE             NOT NULL,

    -- Usage limits
    active                       BOOLEAN          NOT NULL DEFAULT TRUE,
    usage_limit                  INTEGER,                     -- NULL = unlimited
    current_usage                INTEGER          NOT NULL DEFAULT 0,
    per_user_usage_limit         INTEGER          NOT NULL DEFAULT 1,

    -- User constraints
    new_users_only               BOOLEAN          NOT NULL DEFAULT FALSE,

    -- Booking constraints
    min_booking_duration_minutes INTEGER,                     -- NULL = no restriction
    valid_day_type               VARCHAR(20),                 -- WEEKDAY | WEEKEND | ALL | NULL = ALL

    created_at                   TIMESTAMPTZ      NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_coupon_code UNIQUE (code)
);

CREATE INDEX IF NOT EXISTS idx_coupon_code ON coupons (code);

-- ── B2. coupon_applicable_services ───────────────────────────────────────────
-- If rows exist for a coupon → restricted to those service IDs only.
-- Empty = valid for ALL services.
CREATE TABLE IF NOT EXISTS coupon_applicable_services (
    coupon_id  BIGINT NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    service_id BIGINT NOT NULL,
    PRIMARY KEY (coupon_id, service_id)
);

-- ── B3. coupon_applicable_resources ──────────────────────────────────────────
-- If rows exist for a coupon → restricted to those resource IDs only.
CREATE TABLE IF NOT EXISTS coupon_applicable_resources (
    coupon_id   BIGINT NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    resource_id BIGINT NOT NULL,
    PRIMARY KEY (coupon_id, resource_id)
);

-- ── B4. coupon_applicable_activities ─────────────────────────────────────────
-- If rows exist for a coupon → restricted to those activity codes only.
CREATE TABLE IF NOT EXISTS coupon_applicable_activities (
    coupon_id     BIGINT      NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    activity_code VARCHAR(50) NOT NULL,
    PRIMARY KEY (coupon_id, activity_code)
);

-- ── B5. coupon_usages ─────────────────────────────────────────────────────────
-- One row per booking redemption.
-- No unique constraint on (user_id, coupon_id) — multi-use coupons are
-- supported; the per-user limit is enforced in application code.
CREATE TABLE IF NOT EXISTS coupon_usages (
    id         BIGSERIAL   PRIMARY KEY,
    coupon_id  BIGINT      NOT NULL REFERENCES coupons(id)   ON DELETE CASCADE,
    user_id    BIGINT      NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
    booking_id BIGINT      NOT NULL REFERENCES bookings(id)  ON DELETE CASCADE,
    used_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Drop old unique constraint if it exists (prevents multi-use coupons)
ALTER TABLE coupon_usages
    DROP CONSTRAINT IF EXISTS uk_user_coupon;

CREATE INDEX IF NOT EXISTS idx_coupon_usage_user_coupon ON coupon_usages (user_id, coupon_id);

-- ════════════════════════════════════════════════════════════════════════════
-- C. bookings — discount columns
-- ════════════════════════════════════════════════════════════════════════════

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS discount_amount     NUMERIC(19, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS applied_coupon_code VARCHAR(255);

-- ════════════════════════════════════════════════════════════════════════════
-- D. services — resource selection mode
-- ════════════════════════════════════════════════════════════════════════════
-- Controls whether the backend auto-allocates a resource (AUTO, default)
-- or the user explicitly picks one when booking (MANUAL).
-- All existing rows receive 'AUTO' to preserve current behaviour.

ALTER TABLE services
    ADD COLUMN IF NOT EXISTS resource_selection_mode VARCHAR(50) NOT NULL DEFAULT 'AUTO';

COMMENT ON COLUMN services.resource_selection_mode IS
    'AUTO: backend allocates resource via priority algorithm. '
    'MANUAL: user explicitly selects a resource when booking.';

CREATE INDEX IF NOT EXISTS idx_service_resource_selection_mode
    ON services (resource_selection_mode);

-- ============================================================================
-- END OF V2
-- ============================================================================

