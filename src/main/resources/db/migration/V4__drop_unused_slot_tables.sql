-- V4__drop_unused_slot_tables.sql
-- Drop unused slot-related tables (replaced by resource_slots and slot_config)

DROP TABLE IF EXISTS service_slot_booked_dates CASCADE;
DROP TABLE IF EXISTS service_slots CASCADE;
DROP TABLE IF EXISTS slots CASCADE;
