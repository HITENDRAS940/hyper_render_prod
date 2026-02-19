-- ============================================================================
-- V17: Insert initial reference data (Activities only)
-- ============================================================================
-- This migration ensures that essential reference data exists in the database
-- Activities are required for the system to function
--
-- NOTE: Expense categories are NOT inserted here because after V12, they require
-- admin_profile_id and are admin-specific. They will be initialized via
-- DataInitializer when admins are created.
-- ============================================================================

-- Insert Activities (only if not exists)
INSERT INTO activities (code, name, enabled)
SELECT code, name, enabled FROM (
    VALUES
        ('FOOTBALL', 'Football', true),
        ('CRICKET', 'Cricket', true),
        ('BOWLING', 'Bowling', true),
        ('PADEL', 'Padel Ball', true),
        ('BADMINTON', 'Badminton', true),
        ('TENNIS', 'Tennis', true),
        ('SWIMMING', 'Swimming', true),
        ('BASKETBALL', 'Basketball', true),
        ('ARCADE', 'Arcade', true),
        ('GYM', 'Gym', true),
        ('SPA', 'Spa', true),
        ('STUDIO', 'Studio', true),
        ('CONFERENCE', 'Conference', true),
        ('PARTY_HALL', 'Party Hall', true)
) AS new_activities(code, name, enabled)
WHERE NOT EXISTS (
    SELECT 1 FROM activities WHERE activities.code = new_activities.code
);

-- Add informational comment
COMMENT ON TABLE activities IS 'Reference data for sports activities supported by the platform';



