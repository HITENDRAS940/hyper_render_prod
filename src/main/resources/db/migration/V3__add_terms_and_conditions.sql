-- ============================================================================
-- V3__add_terms_and_conditions.sql
-- ============================================================================
-- Adds a TEXT column `terms_and_conditions` to the `services` table.
--
-- Design decisions:
--   • TEXT (not VARCHAR) — no artificial length cap; content can be 1000+ words.
--   • NULL by default    — existing services are unaffected; a NULL value means
--                          "no custom terms set for this service".
--   • Idempotent         — ADD COLUMN IF NOT EXISTS is safe to re-run.
-- ============================================================================

ALTER TABLE services
    ADD COLUMN IF NOT EXISTS terms_and_conditions TEXT DEFAULT NULL;

COMMENT ON COLUMN services.terms_and_conditions IS
    'Full terms and conditions text for this service (plain text or Markdown). '
    'NULL means the service has no custom terms. '
    'Can be several thousand words — stored as TEXT with no length limit.';

