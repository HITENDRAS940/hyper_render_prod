-- V2__add_resource_slots_table.sql
-- Add missing resource_slots table

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
