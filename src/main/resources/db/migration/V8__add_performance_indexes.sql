-- V8: Add performance indexes for frequently queried columns
-- These indexes improve query performance for scheduled jobs and common queries

-- Index for PaymentTimeoutScheduler: frequently queries AWAITING_CONFIRMATION with lockExpiresAt
-- This prevents full table scans when checking for expired bookings
CREATE INDEX IF NOT EXISTS idx_bookings_status_lock_expires 
ON bookings(status, lock_expires_at) 
WHERE status = 'AWAITING_CONFIRMATION' AND lock_expires_at IS NOT NULL;

-- Index for booking date and resource queries (slot availability checks)
-- Used frequently in ResourceSlotService.getSlotAvailability()
CREATE INDEX IF NOT EXISTS idx_bookings_resource_date 
ON bookings(resource_id, booking_date);

-- Index for service city searches (common user query)
-- Used in ServiceService.getServicesByCity()
CREATE INDEX IF NOT EXISTS idx_services_city 
ON services(LOWER(city));

-- Index for booking status queries (admin dashboard and filters)
CREATE INDEX IF NOT EXISTS idx_bookings_status 
ON bookings(status);

-- Index for payment-pending bookings with lock expiration
-- Used by BookingExpiryScheduler
CREATE INDEX IF NOT EXISTS idx_bookings_payment_pending_lock 
ON bookings(status, lock_expires_at) 
WHERE status = 'PAYMENT_PENDING' AND lock_expires_at IS NOT NULL;

-- Composite index for service resources lookups
-- Used in SlotBookingService for activity and service filtering
CREATE INDEX IF NOT EXISTS idx_service_resources_service_enabled 
ON service_resources(service_id, enabled);

-- Index for activity code lookups
-- Activities are frequently looked up by code
CREATE INDEX IF NOT EXISTS idx_activities_code 
ON activities(code);

-- Add comments for documentation
COMMENT ON INDEX idx_bookings_status_lock_expires IS 'Optimizes PaymentTimeoutScheduler queries for expired AWAITING_CONFIRMATION bookings';
COMMENT ON INDEX idx_bookings_resource_date IS 'Optimizes slot availability queries by resource and date';
COMMENT ON INDEX idx_services_city IS 'Optimizes service searches by city (case-insensitive)';
COMMENT ON INDEX idx_bookings_status IS 'Optimizes status-based filtering queries';
COMMENT ON INDEX idx_bookings_payment_pending_lock IS 'Optimizes BookingExpiryScheduler queries for expired PAYMENT_PENDING bookings';
COMMENT ON INDEX idx_service_resources_service_enabled IS 'Optimizes service resource lookups with enabled filter';
COMMENT ON INDEX idx_activities_code IS 'Optimizes activity lookups by code';
