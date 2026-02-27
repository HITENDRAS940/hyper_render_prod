-- ============================================================================
-- V2__widen_services_contact_number.sql
-- ============================================================================
-- The contact_number column in services was VARCHAR(20) which is too short
-- for phone numbers with country codes, spaces, or dashes (e.g. +91 98765 43210).
-- Widen it to VARCHAR(50) to accommodate all realistic formats.
-- ============================================================================

ALTER TABLE services ALTER COLUMN contact_number TYPE VARCHAR(50);

