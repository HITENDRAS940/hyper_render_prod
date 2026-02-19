package com.hitendra.turf_booking_backend.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for refund rules.
 * Rules are ordered by minMinutesBefore in descending order.
 *
 * NEW POLICY (Updated Feb 2026):
 * - Cancel 12+ hours (720 minutes) before slot = 100% refund
 * - Cancel within 12 hours = 0% refund (no refund)
 *
 * Example config in application.properties:
 * refund.rules[0].minMinutesBefore=720
 * refund.rules[0].refundPercent=100
 * refund.rules[1].minMinutesBefore=0
 * refund.rules[1].refundPercent=0
 */
@Configuration
@ConfigurationProperties(prefix = "refund")
@Data
public class RefundConfig {

    /**
     * Whether refunds are enabled
     */
    private boolean enabled = true;

    /**
     * Minimum minutes before slot when cancellation is allowed
     * Set to -1 to allow cancellation anytime (even after slot starts)
     */
    private int minMinutesForCancellation = 0;

    /**
     * Whether to allow cancellation for past bookings (booking date has passed)
     */
    private boolean allowPastCancellation = false;

    /**
     * Refund rules list - applied in order
     */
    private List<RefundRule> rules = List.of(
            new RefundRule(120, 100),  // 2+ hours before: 100% refund
            new RefundRule(30, 50),    // 30 mins to 2 hours: 50% refund
            new RefundRule(0, 0)       // Less than 30 mins: 0% refund
    );

    /**
     * Single refund rule
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundRule {
        /**
         * Minimum minutes before slot start time for this rule to apply
         */
        private int minMinutesBefore;

        /**
         * Refund percentage for this rule (0-100)
         */
        private int refundPercent;
    }

    /**
     * Get refund percentage based on minutes before slot.
     * Rules are checked in order - first matching rule wins.
     *
     * @param minutesBeforeSlot minutes remaining before slot starts
     * @return refund percentage (0-100)
     */
    public int getRefundPercent(long minutesBeforeSlot) {
        for (RefundRule rule : rules) {
            if (minutesBeforeSlot >= rule.getMinMinutesBefore()) {
                return rule.getRefundPercent();
            }
        }
        return 0; // Default: no refund
    }
}
