-- ============================================================================
-- V3__add_app_config.sql
-- ============================================================================
-- Creates the app_config table and seeds the initial configuration with
-- minVersion 6.1.0 and latestVersion 6.1.0 for both iOS and Android.
-- ============================================================================

CREATE TABLE IF NOT EXISTS app_config (
    id                   BIGSERIAL PRIMARY KEY,
    ios_min_version      VARCHAR(20)   NOT NULL,
    ios_latest_version   VARCHAR(20)   NOT NULL,
    ios_store_url        VARCHAR(500)  NOT NULL,
    android_min_version  VARCHAR(20)   NOT NULL,
    android_latest_version VARCHAR(20) NOT NULL,
    android_store_url    VARCHAR(500)  NOT NULL,
    force_update_message VARCHAR(1000) NOT NULL,
    soft_update_message  VARCHAR(1000) NOT NULL
);

-- Seed the default configuration
INSERT INTO app_config (
    ios_min_version,
    ios_latest_version,
    ios_store_url,
    android_min_version,
    android_latest_version,
    android_store_url,
    force_update_message,
    soft_update_message
) VALUES (
    '6.1.0',
    '6.1.0',
    'https://apps.apple.com/app/hyper/idXXXXXXXXX',
    '6.1.0',
    '6.1.0',
    'https://play.google.com/store/apps/details?id=com.hitendras940.hyper',
    'A critical update is required to continue using Hyper. Please update to the latest version.',
    'A new version of Hyper is available with premium UI enhancements and bug fixes!'
);

