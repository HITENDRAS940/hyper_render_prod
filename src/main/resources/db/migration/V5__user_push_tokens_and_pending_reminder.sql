-- ============================================================================
-- V5__user_push_tokens_and_pending_reminder.sql
-- ============================================================================
-- 1) Adds user push-token storage for mobile notifications.
-- 2) Adds reminder marker on bookings so pending reminders are sent once.
-- ============================================================================

CREATE TABLE IF NOT EXISTS user_push_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_user_push_tokens_user_id ON user_push_tokens(user_id);

ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS pending_reminder_sent_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_booking_pending_reminder
    ON bookings(status, pending_reminder_sent_at, created_at);

