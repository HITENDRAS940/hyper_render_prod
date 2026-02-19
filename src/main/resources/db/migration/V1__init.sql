-- ============================================================================
-- V1__init.sql - Complete Database Schema for Hyper Turf Booking Backend
-- ============================================================================
-- This migration creates ALL tables, constraints, and indexes for the application.
-- Compatible with PostgreSQL.
-- Generated: February 19, 2026
-- ============================================================================

-- ============================================================================
-- USERS & AUTHENTICATION
-- ============================================================================

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    address TEXT,
    gstin VARCHAR(50),
    oauth_provider VARCHAR(20),
    oauth_provider_id VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_account_status ON users(account_status);

-- User Profiles
CREATE TABLE IF NOT EXISTS user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    address TEXT,
    date_of_birth DATE,
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(20),
    profile_image_url VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id ON user_profiles(user_id);

-- Admin Profiles
CREATE TABLE IF NOT EXISTS admin_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    city VARCHAR(100),
    business_name VARCHAR(255),
    business_address TEXT,
    gst_number VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_admin_profiles_user_id ON admin_profiles(user_id);

-- OTPs for authentication
CREATE TABLE IF NOT EXISTS otps (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(20),
    email VARCHAR(255),
    code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_otps_phone ON otps(phone);
CREATE INDEX IF NOT EXISTS idx_otps_email ON otps(email);
CREATE INDEX IF NOT EXISTS idx_otps_expires_at ON otps(expires_at);

-- Admin Push Tokens (for notifications)
CREATE TABLE IF NOT EXISTS admin_push_tokens (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_admin_push_tokens_admin_id ON admin_push_tokens(admin_id);

-- ============================================================================
-- ACTIVITIES (Sports/Games)
-- ============================================================================

CREATE TABLE IF NOT EXISTS activities (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_activities_code ON activities(code);

-- ============================================================================
-- SERVICES (Turf/Venue)
-- ============================================================================

CREATE TABLE IF NOT EXISTS services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location TEXT,
    city VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    description TEXT,
    contact_number VARCHAR(20),
    gstin VARCHAR(50),
    state VARCHAR(100),
    start_time TIME,
    end_time TIME,
    availability BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_admin_id BIGINT REFERENCES admin_profiles(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_services_city ON services(city);
CREATE INDEX IF NOT EXISTS idx_services_created_by ON services(created_by_admin_id);

-- Service Amenities (ElementCollection)
CREATE TABLE IF NOT EXISTS service_amenities (
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    amenity VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_service_amenities_service ON service_amenities(service_id);

-- Service Images (ElementCollection)
CREATE TABLE IF NOT EXISTS service_images (
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_service_images_service ON service_images(service_id);

-- Service-Activity Join Table (ManyToMany)
CREATE TABLE IF NOT EXISTS service_activity (
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    activity_id BIGINT NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    PRIMARY KEY (service_id, activity_id)
);

-- ============================================================================
-- SERVICE RESOURCES (Individual Turfs/Courts within a Service)
-- ============================================================================

CREATE TABLE IF NOT EXISTS service_resources (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_service_resources_service ON service_resources(service_id);

-- Resource-Activity Join Table (ManyToMany)
CREATE TABLE IF NOT EXISTS resource_activities (
    resource_id BIGINT NOT NULL REFERENCES service_resources(id) ON DELETE CASCADE,
    activity_id BIGINT NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    PRIMARY KEY (resource_id, activity_id)
);

-- ============================================================================
-- SLOT CONFIGURATION & PRICING
-- ============================================================================

CREATE TABLE IF NOT EXISTS resource_slot_configs (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL UNIQUE REFERENCES service_resources(id) ON DELETE CASCADE,
    opening_time TIME NOT NULL,
    closing_time TIME NOT NULL,
    slot_duration_minutes INTEGER NOT NULL,
    base_price DOUBLE PRECISION NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_slot_configs_resource ON resource_slot_configs(resource_id);

-- Resource Slots (pre-generated slots for fast access)
CREATE TABLE IF NOT EXISTS resource_slots (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL REFERENCES service_resources(id) ON DELETE CASCADE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    display_name VARCHAR(100),
    base_price DOUBLE PRECISION NOT NULL,
    weekday_price DOUBLE PRECISION,
    weekend_price DOUBLE PRECISION,
    display_order INTEGER,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    duration_minutes INTEGER NOT NULL,
    UNIQUE (resource_id, start_time, end_time)
);

CREATE INDEX IF NOT EXISTS idx_resource_slots_resource ON resource_slots(resource_id);

-- Dynamic pricing rules for slots
CREATE TABLE IF NOT EXISTS resource_price_rules (
    id BIGSERIAL PRIMARY KEY,
    resource_slot_config_id BIGINT NOT NULL REFERENCES resource_slot_configs(id) ON DELETE CASCADE,
    day_type VARCHAR(20) NOT NULL DEFAULT 'ALL',
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    base_price DOUBLE PRECISION,
    extra_charge DOUBLE PRECISION DEFAULT 0.0,
    reason VARCHAR(255),
    priority INTEGER NOT NULL DEFAULT 1,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_price_rules_config ON resource_price_rules(resource_slot_config_id);

-- ============================================================================
-- DISABLED SLOTS (Blocked time slots)
-- ============================================================================

CREATE TABLE IF NOT EXISTS disabled_slots (
    id BIGSERIAL PRIMARY KEY,
    resource_id BIGINT NOT NULL REFERENCES service_resources(id) ON DELETE CASCADE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    disabled_date DATE NOT NULL,
    reason VARCHAR(500),
    disabled_by_admin_id BIGINT REFERENCES admin_profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (resource_id, start_time, disabled_date)
);

CREATE INDEX IF NOT EXISTS idx_disabled_slots_resource_date ON disabled_slots(resource_id, disabled_date);

-- ============================================================================
-- BOOKINGS
-- ============================================================================

CREATE TABLE IF NOT EXISTS bookings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    service_id BIGINT REFERENCES services(id) ON DELETE SET NULL,
    resource_id BIGINT REFERENCES service_resources(id) ON DELETE SET NULL,
    activity_code VARCHAR(50),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    booking_date DATE NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reference VARCHAR(100) UNIQUE,
    idempotency_key VARCHAR(100) UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_expires_at TIMESTAMP WITH TIME ZONE,
    payment_mode VARCHAR(50),
    payment_source VARCHAR(30),
    created_by_admin_id BIGINT REFERENCES admin_profiles(id) ON DELETE SET NULL,
    razorpay_order_id VARCHAR(100),
    razorpay_payment_id VARCHAR(100),
    razorpay_signature VARCHAR(500),
    payment_method VARCHAR(50),
    payment_status VARCHAR(20) DEFAULT 'NOT_STARTED',
    payment_time TIMESTAMP WITH TIME ZONE,
    payment_initiated_at TIMESTAMP WITH TIME ZONE,
    online_amount_paid NUMERIC(19, 2),
    venue_amount_due NUMERIC(19, 2),
    venue_amount_collected BOOLEAN DEFAULT FALSE,
    venue_payment_collection_method VARCHAR(20),
    advance_amount NUMERIC(19, 2),
    remaining_amount NUMERIC(19, 2),
    transfer_status VARCHAR(20) DEFAULT 'PENDING'
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_booking_idempotency_key ON bookings(idempotency_key);
CREATE INDEX IF NOT EXISTS idx_booking_resource_date ON bookings(resource_id, booking_date);
CREATE INDEX IF NOT EXISTS idx_booking_service_date ON bookings(service_id, booking_date);
CREATE INDEX IF NOT EXISTS idx_booking_status_lock_expires ON bookings(status, lock_expires_at);
CREATE INDEX IF NOT EXISTS idx_booking_user ON bookings(user_id);
CREATE INDEX IF NOT EXISTS idx_booking_status ON bookings(status);
CREATE INDEX IF NOT EXISTS idx_booking_date ON bookings(booking_date);
CREATE INDEX IF NOT EXISTS idx_booking_razorpay_order ON bookings(razorpay_order_id);

-- ============================================================================
-- REFUNDS
-- ============================================================================

CREATE TABLE IF NOT EXISTS refunds (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    service_id BIGINT REFERENCES services(id) ON DELETE SET NULL,
    original_amount NUMERIC(19, 2) NOT NULL,
    refund_amount NUMERIC(19, 2) NOT NULL,
    refund_percent INTEGER NOT NULL,
    minutes_before_slot BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INITIATED',
    razorpay_refund_id VARCHAR(100),
    razorpay_payment_id VARCHAR(100),
    wallet_transaction_id BIGINT,
    refund_type VARCHAR(20) NOT NULL,
    reason TEXT,
    initiated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR'
);

CREATE INDEX IF NOT EXISTS idx_refund_booking ON refunds(booking_id);
CREATE INDEX IF NOT EXISTS idx_refund_razorpay_id ON refunds(razorpay_refund_id);
CREATE INDEX IF NOT EXISTS idx_refund_status ON refunds(status);
CREATE INDEX IF NOT EXISTS idx_refund_user ON refunds(user_id);
CREATE INDEX IF NOT EXISTS idx_refund_service ON refunds(service_id);

-- ============================================================================
-- PROCESSED PAYMENTS (Idempotency tracking)
-- ============================================================================

CREATE TABLE IF NOT EXISTS processed_payments (
    id BIGSERIAL PRIMARY KEY,
    cf_payment_id VARCHAR(100) NOT NULL UNIQUE,
    order_id VARCHAR(100) NOT NULL,
    internal_booking_id VARCHAR(100) NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    payment_status VARCHAR(30) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_processed_payments_order ON processed_payments(order_id);
CREATE INDEX IF NOT EXISTS idx_processed_payments_booking ON processed_payments(internal_booking_id);

-- ============================================================================
-- ACCOUNTING - EXPENSE CATEGORIES
-- ============================================================================

CREATE TABLE IF NOT EXISTS expense_categories (
    id BIGSERIAL PRIMARY KEY,
    admin_profile_id BIGINT NOT NULL REFERENCES admin_profiles(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_expense_category_admin_name UNIQUE (admin_profile_id, name)
);

CREATE INDEX IF NOT EXISTS idx_expense_category_admin ON expense_categories(admin_profile_id);

-- ============================================================================
-- ACCOUNTING - EXPENSES
-- ============================================================================

CREATE TABLE IF NOT EXISTS expenses (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    category VARCHAR(255) NOT NULL,
    description VARCHAR(500) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    payment_mode VARCHAR(20),
    expense_date DATE NOT NULL,
    bill_url VARCHAR(500),
    created_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_service_expense_date ON expenses(service_id, expense_date);
CREATE INDEX IF NOT EXISTS idx_expense_date ON expenses(expense_date);

-- ============================================================================
-- ACCOUNTING - CASH LEDGER
-- ============================================================================

CREATE TABLE IF NOT EXISTS cash_ledger (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    source VARCHAR(30) NOT NULL,
    reference_type VARCHAR(30) NOT NULL,
    reference_id BIGINT NOT NULL,
    credit_amount DOUBLE PRECISION DEFAULT 0.0,
    debit_amount DOUBLE PRECISION DEFAULT 0.0,
    balance_after DOUBLE PRECISION NOT NULL,
    payment_mode VARCHAR(20),
    description TEXT,
    recorded_by VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_service_created ON cash_ledger(service_id, created_at);
CREATE INDEX IF NOT EXISTS idx_source_reference ON cash_ledger(source, reference_type, reference_id);

-- ============================================================================
-- REFERENCE DATA - ACTIVITIES
-- ============================================================================
-- These activities match DataInitializer.java to ensure consistency
-- ON CONFLICT DO NOTHING allows safe re-running and prevents duplicates

INSERT INTO activities (code, name, enabled) VALUES
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
ON CONFLICT (code) DO NOTHING;

-- ============================================================================
-- END OF COMPREHENSIVE SCHEMA
-- ============================================================================

