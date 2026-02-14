-- Invoice Generation Diagnostic Queries
-- Run these queries to diagnose invoice generation issues

-- ═══════════════════════════════════════════════════════════════════════
-- 1. Check if any invoices exist
-- ═══════════════════════════════════════════════════════════════════════
SELECT
    COUNT(*) as total_invoices,
    MIN(created_at) as first_invoice,
    MAX(created_at) as latest_invoice
FROM invoices;

-- ═══════════════════════════════════════════════════════════════════════
-- 2. Check confirmed bookings WITHOUT invoices (PROBLEM BOOKINGS)
-- ═══════════════════════════════════════════════════════════════════════
SELECT
    b.id,
    b.reference,
    b.status,
    b.payment_status,
    b.amount,
    b.created_at as booking_created,
    b.payment_time,
    u.name as customer_name,
    u.email as customer_email,
    s.name as service_name
FROM bookings b
LEFT JOIN invoices i ON b.id = i.booking_id
LEFT JOIN users u ON b.user_id = u.id
LEFT JOIN services s ON b.service_id = s.id
WHERE b.status = 'CONFIRMED'
  AND i.id IS NULL
ORDER BY b.created_at DESC
LIMIT 50;

-- ═══════════════════════════════════════════════════════════════════════
-- 3. Check all confirmed bookings (with invoice status)
-- ═══════════════════════════════════════════════════════════════════════
SELECT
    b.id,
    b.reference,
    b.status,
    b.payment_status,
    b.amount,
    b.created_at as booking_created,
    CASE
        WHEN i.id IS NOT NULL THEN '✅ HAS INVOICE'
        ELSE '❌ NO INVOICE'
    END as invoice_status,
    i.invoice_number,
    i.cloudinary_url,
    i.created_at as invoice_created
FROM bookings b
LEFT JOIN invoices i ON b.id = i.booking_id
WHERE b.status = 'CONFIRMED'
ORDER BY b.created_at DESC
LIMIT 50;

-- ═══════════════════════════════════════════════════════════════════════
-- 4. Count bookings by status and invoice presence
-- ═══════════════════════════════════════════════════════════════════════
SELECT
    b.status,
    COUNT(*) as total_bookings,
    COUNT(i.id) as bookings_with_invoice,
    COUNT(*) - COUNT(i.id) as bookings_without_invoice,
    ROUND(100.0 * COUNT(i.id) / COUNT(*), 2) as invoice_generation_rate
FROM bookings b
LEFT JOIN invoices i ON b.id = i.booking_id
GROUP BY b.status
ORDER BY b.status;

-- ═══════════════════════════════════════════════════════════════════════
-- 5. Check invoice template configuration
-- ═══════════════════════════════════════════════════════════════════════
SELECT
    id,
    name,
    version,
    is_active,
    created_by,
    created_at,
    updated_at,
    LENGTH(content) as template_size_bytes
FROM invoice_templates
ORDER BY version DESC;

-- ═══════════════════════════════════════════════════════════════════════
-- 6. Check active invoice template (must exist)
-- ═══════════════════════════════════════════════════════════════════════
SELECT
    CASE
        WHEN EXISTS (SELECT 1 FROM invoice_templates WHERE is_active = true)
        THEN '✅ Active template EXISTS'
        ELSE '❌ NO ACTIVE TEMPLATE - Invoice generation will FAIL'
    END as template_status,
    (SELECT COUNT(*) FROM invoice_templates) as total_templates,
    (SELECT COUNT(*) FROM invoice_templates WHERE is_active = true) as active_templates;

-- ═══════════════════════════════════════════════════════════════════════
-- 7. Recent bookings with detailed status
-- ═══════════════════════════════════════════════════════════════════════
SELECT
    b.id,
    b.reference,
    b.status,
    b.payment_status,
    b.razorpay_order_id,
    b.razorpay_payment_id,
    b.created_at,
    b.payment_time,
    EXTRACT(EPOCH FROM (b.payment_time - b.created_at)) as payment_delay_seconds,
    i.id as invoice_id,
    i.invoice_number,
    i.created_at as invoice_created_at,
    CASE
        WHEN i.id IS NOT NULL THEN
            EXTRACT(EPOCH FROM (i.created_at - b.payment_time))
        ELSE NULL
    END as invoice_generation_delay_seconds
FROM bookings b
LEFT JOIN invoices i ON b.id = i.booking_id
WHERE b.status IN ('CONFIRMED', 'AWAITING_CONFIRMATION')
ORDER BY b.created_at DESC
LIMIT 20;

-- ═══════════════════════════════════════════════════════════════════════
-- 8. Invoices with template version tracking
-- ═══════════════════════════════════════════════════════════════════════
SELECT
    i.id,
    i.booking_id,
    i.invoice_number,
    i.invoice_amount,
    i.template_version,
    i.created_at,
    LENGTH(i.cloudinary_url) as url_length,
    SUBSTRING(i.cloudinary_url FROM 1 FOR 50) as cloudinary_url_preview
FROM invoices i
ORDER BY i.created_at DESC
LIMIT 20;

-- ═══════════════════════════════════════════════════════════════════════
-- 9. Find bookings confirmed in last 24 hours without invoices
-- ═══════════════════════════════════════════════════════════════════════
SELECT
    b.id,
    b.reference,
    b.amount,
    b.payment_time,
    NOW() - b.payment_time as time_since_payment,
    u.name as customer_name,
    u.email
FROM bookings b
LEFT JOIN invoices i ON b.id = i.booking_id
LEFT JOIN users u ON b.user_id = u.id
WHERE b.status = 'CONFIRMED'
  AND b.payment_time > NOW() - INTERVAL '24 hours'
  AND i.id IS NULL
ORDER BY b.payment_time DESC;

-- ═══════════════════════════════════════════════════════════════════════
-- 10. Summary Statistics
-- ═══════════════════════════════════════════════════════════════════════
SELECT
    'Total Bookings' as metric,
    COUNT(*) as value
FROM bookings
UNION ALL
SELECT
    'Confirmed Bookings',
    COUNT(*)
FROM bookings
WHERE status = 'CONFIRMED'
UNION ALL
SELECT
    'Total Invoices',
    COUNT(*)
FROM invoices
UNION ALL
SELECT
    'Confirmed Bookings WITHOUT Invoices',
    COUNT(*)
FROM bookings b
LEFT JOIN invoices i ON b.id = i.booking_id
WHERE b.status = 'CONFIRMED' AND i.id IS NULL
UNION ALL
SELECT
    'Invoice Generation Success Rate (%)',
    ROUND(100.0 * COUNT(i.id) / NULLIF(COUNT(b.id), 0), 2)
FROM bookings b
LEFT JOIN invoices i ON b.id = i.booking_id
WHERE b.status = 'CONFIRMED';

-- ═══════════════════════════════════════════════════════════════════════
-- REMEDIATION: Get list of booking IDs to regenerate invoices for
-- ═══════════════════════════════════════════════════════════════════════
-- Use this output to call POST /api/invoices/regenerate/{bookingId}
SELECT
    'POST /api/invoices/regenerate/' || b.id as api_endpoint,
    b.id as booking_id,
    b.reference,
    b.amount,
    b.payment_time
FROM bookings b
LEFT JOIN invoices i ON b.id = i.booking_id
WHERE b.status = 'CONFIRMED'
  AND i.id IS NULL
ORDER BY b.payment_time DESC;

