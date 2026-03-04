-- ============================================================================
-- V4__add_min_person_allowed.sql
-- ============================================================================
-- Adds `min_person_allowed` column to `service_resources` table.
--
-- Design:
--   • Mirrors the existing `max_person_allowed` column.
--   • NULL means no lower bound — a single person is sufficient.
--   • Only meaningful when pricing_type = 'PER_PERSON'.
--   • Idempotent: ADD COLUMN IF NOT EXISTS is safe to re-run.
-- ============================================================================

ALTER TABLE service_resources
    ADD COLUMN IF NOT EXISTS min_person_allowed INTEGER DEFAULT NULL;

COMMENT ON COLUMN service_resources.min_person_allowed IS
    'Minimum number of persons required per booking. '
    'Only relevant when pricing_type = ''PER_PERSON''. '
    'NULL means no lower bound (a single person is sufficient).';

