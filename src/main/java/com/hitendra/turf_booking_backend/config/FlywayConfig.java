package com.hitendra.turf_booking_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Flyway migration strategy that:
 * 1. Deletes incorrectly recorded SUCCESS entries for migrations that reference
 *    columns which do not actually exist in the database (V5, V6, V7 were
 *    recorded as SUCCESS on some databases before the DDL executed).
 * 2. Runs repair() to clean up any FAILED entries.
 * 3. Runs migrate() to re-apply any missing migrations.
 */
@Configuration
@Slf4j
public class FlywayConfig {

    private final DataSource dataSource;

    public FlywayConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {

            try (Connection conn = dataSource.getConnection()) {

                // ── Fix 1: admin_profiles missing financial columns → re-run V5/V6/V7/V8 ──
                if (adminProfilesMissingFinancialColumns(conn)) {
                    log.warn("⚠️  admin_profiles is missing financial columns — deleting V5/V6/V7/V8 " +
                             "from flyway_schema_history so they are re-applied...");
                    deleteFlywayHistoryEntries(conn, "5", "6", "7", "8");
                    log.info("✅ Flyway history entries deleted. Migrations will be re-applied.");
                } else {
                    log.info("✅ admin_profiles financial columns are present — no history fix needed.");
                }

                // ── Fix 2: coupons table missing enhanced constraint columns → re-run V2/V3 ──
                if (couponsMissingEnhancedColumns(conn)) {
                    log.warn("⚠️  coupons table is missing enhanced constraint columns — deleting V2/V3 " +
                             "from flyway_schema_history so they are re-applied...");
                    deleteFlywayHistoryEntries(conn, "2", "3");
                    log.info("✅ Flyway V2/V3 history entries deleted. Migrations will be re-applied.");
                } else {
                    log.info("✅ coupons enhanced columns are present — no history fix needed.");
                }

            } catch (Exception e) {
                log.warn("⚠️  Could not check/fix flyway_schema_history (non-fatal): {}", e.getMessage());
            }

            // ── Standard repair (clears FAILED entries) ──────────────────────────
            log.info("Running Flyway repair...");
            try {
                flyway.repair();
                log.info("Flyway repair completed.");
            } catch (Exception e) {
                log.warn("Flyway repair encountered an issue (non-fatal): {}", e.getMessage());
            }

            // ── Migrate ───────────────────────────────────────────────────────────
            log.info("Running Flyway migrate...");
            flyway.migrate();
            log.info("Flyway migration completed.");
        };
    }

    /**
     * Returns true if the coupons table exists but is missing the enhanced
     * constraint columns added in V2/V3 (e.g. min_booking_duration_minutes).
     */
    private boolean couponsMissingEnhancedColumns(Connection conn) {
        try {
            // Check table exists
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM information_schema.tables WHERE table_name = 'coupons'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false; // Fresh DB — migrations will create it
                }
            }
            // Check for the sentinel column that the old minimal schema was missing
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM information_schema.columns " +
                    "WHERE table_name = 'coupons' AND column_name = 'min_booking_duration_minutes'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    return !rs.next(); // Missing → need to re-run migrations
                }
            }
        } catch (Exception e) {
            log.warn("Could not inspect coupons columns: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns true if the admin_profiles table exists but is missing bank_balance.
     * Uses information_schema for reliable detection across all PostgreSQL setups.
     */
    private boolean adminProfilesMissingFinancialColumns(Connection conn) {
        try {
            // Check if admin_profiles table exists
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM information_schema.tables WHERE table_name = 'admin_profiles'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return false; // Table doesn't exist yet — fresh DB, migrations will create it
                    }
                }
            }
            // Check if bank_balance column exists via information_schema
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM information_schema.columns " +
                    "WHERE table_name = 'admin_profiles' AND column_name = 'bank_balance'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    return !rs.next(); // Missing if no result
                }
            }
        } catch (Exception e) {
            log.warn("Could not inspect admin_profiles columns: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Deletes rows from flyway_schema_history for the given version numbers.
     * Safe — if the table or rows don't exist, it silently continues.
     */
    private void deleteFlywayHistoryEntries(Connection conn, String... versions) {
        for (String version : versions) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM flyway_schema_history WHERE version = ?")) {
                ps.setString(1, version);
                int deleted = ps.executeUpdate();
                if (deleted > 0) {
                    log.info("  Deleted flyway_schema_history entry for V{}", version);
                }
            } catch (Exception e) {
                log.warn("  Could not delete flyway_schema_history entry for V{}: {}", version, e.getMessage());
            }
        }
    }
}

