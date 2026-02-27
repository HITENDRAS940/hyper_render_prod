package com.hitendra.turf_booking_backend.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for slot availability by activity.
 * Returns aggregated slots across all compatible (identical) resources.
 * Optimized for low latency and minimal payload size.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlotAvailabilityResponseDto {

    /**
     * Pricing strategy of the resource pool for this activity.
     * "PER_SLOT"   — price is flat per slot; numberOfPersons is not required.
     * "PER_PERSON" — price is multiplied by numberOfPersons; frontend should
     *                prompt the user to enter headcount before booking.
     */
    private String pricingType;

    /**
     * Maximum number of persons allowed per booking.
     * Only populated when pricingType = "PER_PERSON".
     * Null means no upper limit.
     */
    private Integer maxPersonAllowed;

    /**
     * List of aggregated slots with availability counts
     */
    private List<SlotDto> slots;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SlotDto {
        /**
         * Encrypted payload containing booking intent (opaque to frontend)
         */
        private String slotKey;

        /**
         * Deterministic hash for UI identity and idempotency
         */
        private String slotGroupId;

        private String startTime;
        private String endTime;
        private Integer durationMinutes;
        private Double displayPrice;

        private Integer availableCount;
        private Integer totalCount;
        private Boolean available;
    }
}


