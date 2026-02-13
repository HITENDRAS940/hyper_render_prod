-- ============================================================================
-- FIX ORPHANED BOOKING: created_by_admin_id is NULL
-- ============================================================================
-- This script fixes the booking BKCD9C4047 that was created without admin profile
--
-- BEFORE RUNNING: Check if admin profile exists!
-- ============================================================================

-- STEP 1: Check which admins have profiles and which don't
SELECT
    u.id as user_id,
    u.email,
    u.name,
    u.role,
    ap.id as admin_profile_id,
    CASE
        WHEN ap.id IS NULL THEN '❌ MISSING PROFILE'
        ELSE '✅ HAS PROFILE'
    END as status
FROM users u
LEFT JOIN admin_profiles ap ON ap.user_id = u.id
WHERE u.role = 'ADMIN'
ORDER BY u.id;

-- ============================================================================
-- STEP 2: If admin profile is missing, create it
-- ============================================================================
-- UNCOMMENT AND MODIFY THIS SECTION if admin profile doesn't exist
-- Replace YOUR_USER_ID with the actual user ID from STEP 1

/*
INSERT INTO admin_profiles (user_id, city, business_name, business_address, gst_number)
VALUES (
    YOUR_USER_ID,  -- Replace with actual user ID
    'Bangalore',    -- Replace with actual city
    'Turf Business', -- Replace with actual business name
    '123 Main Street', -- Replace with actual address
    NULL            -- Replace with GST number if available
);
*/

-- ============================================================================
-- STEP 3: Get the admin profile ID
-- ============================================================================
-- After creating profile or if it already exists, get the ID
SELECT
    ap.id as admin_profile_id,
    ap.user_id,
    u.email,
    u.name
FROM admin_profiles ap
JOIN users u ON ap.user_id = u.id
WHERE u.role = 'ADMIN'
ORDER BY ap.id;

-- ============================================================================
-- STEP 4: Check the orphaned booking
-- ============================================================================
SELECT
    id,
    reference,
    service_id,
    resource_id,
    booking_date,
    created_by_admin_id,
    user_id,
    status,
    payment_source,
    idempotency_key
FROM bookings
WHERE reference = 'BKCD9C4047';

-- Expected: created_by_admin_id should be NULL

-- ============================================================================
-- STEP 5: Fix the orphaned booking
-- ============================================================================
-- UNCOMMENT THIS SECTION to fix the booking
-- Replace :adminProfileId with the actual admin_profile.id from STEP 3

/*
UPDATE bookings
SET created_by_admin_id = :adminProfileId  -- Replace with actual admin_profile.id
WHERE reference = 'BKCD9C4047';
*/

-- ============================================================================
-- STEP 6: Verify the fix
-- ============================================================================
-- After updating, verify the booking is now linked to admin profile
SELECT
    id,
    reference,
    created_by_admin_id,
    booking_date,
    status
FROM bookings
WHERE reference = 'BKCD9C4047';

-- Expected: created_by_admin_id should now have a value

-- ============================================================================
-- STEP 7: Test the query that the API uses
-- ============================================================================
-- Replace :adminProfileId with the actual admin_profile.id
-- This simulates what the API does
/*
SELECT
    b.id,
    b.reference,
    b.booking_date,
    b.status,
    CASE
        WHEN b.user_id IS NULL THEN 'MANUAL BOOKING'
        ELSE 'USER BOOKING'
    END as booking_type,
    b.created_by_admin_id,
    s.created_by_id as service_created_by
FROM bookings b
JOIN services s ON b.service_id = s.id
WHERE (
    s.created_by_id = :adminProfileId  -- Service bookings
    OR
    b.created_by_admin_id = :adminProfileId  -- Manual bookings
)
AND b.booking_date = '2026-02-16'
ORDER BY b.created_at DESC;
*/

-- Expected: The booking BKCD9C4047 should appear in results

-- ============================================================================
-- STEP 8: Find all other orphaned bookings (if any)
-- ============================================================================
SELECT
    COUNT(*) as orphaned_count
FROM bookings
WHERE user_id IS NULL
  AND created_by_admin_id IS NULL;

-- If count > 0, you have more orphaned bookings to fix!

-- List all orphaned bookings
SELECT
    id,
    reference,
    booking_date,
    status,
    service_id,
    idempotency_key
FROM bookings
WHERE user_id IS NULL
  AND created_by_admin_id IS NULL
ORDER BY created_at DESC;

-- ============================================================================
-- STEP 9: Fix all orphaned bookings at once (OPTIONAL)
-- ============================================================================
-- UNCOMMENT THIS SECTION to fix ALL orphaned bookings
-- Replace :adminProfileId with the actual admin_profile.id

/*
UPDATE bookings
SET created_by_admin_id = :adminProfileId  -- Replace with actual admin_profile.id
WHERE user_id IS NULL
  AND created_by_admin_id IS NULL
  AND idempotency_key LIKE 'ADMIN-%';  -- Only fix admin-created bookings
*/

-- ============================================================================
-- DONE! Now test the API
-- ============================================================================
-- After running this script, test the API:
-- GET /admin/bookings?date=2026-02-16
--
-- The booking should now appear in the response!
-- ============================================================================

