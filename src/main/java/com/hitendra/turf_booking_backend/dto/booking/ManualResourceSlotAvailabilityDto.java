package com.hitendra.turf_booking_backend.dto.booking;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for the manual resource-selection availability endpoint.
 *
 * Used by services configured with {@code resourceSelectionMode = MANUAL}.
 * Instead of the aggregated pool view returned by the standard availability
 * endpoint, this response exposes each individual resource with its own
 * per-slot availability, allowing the user to choose a specific resource.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManualResourceSlotAvailabilityDto {


    /**
     * Each element represents one bookable resource of the service.
     * Resources are listed in their natural order (by id / name).
     */
    private List<ResourceSlotDto> resources;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Availability information for a single resource.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResourceSlotDto {

        /** Database ID of this resource. */
        private Long resourceId;

        /** Human-readable name, e.g. "Lane 1", "Arcade Machine A". */
        private String resourceName;

        /**
         * Optional free-text description of the resource shown to the user,
         * e.g. "6-pin bowling lane", "Latest gaming rig".
         */
        private String resourceDescription;

        /**
         * Pricing strategy for this specific resource.
         * "PER_SLOT"   – price is flat per slot; numberOfPersons is not required.
         * "PER_PERSON" – price is multiplied by numberOfPersons; frontend should
         *                prompt the user to enter headcount before booking.
         */
        private String pricingType;

        /**
         * Minimum number of persons required per booking.
         * Only populated when pricingType = "PER_PERSON".
         * Null means no lower bound — a single person is sufficient.
         */
        private Integer minPersonAllowed;

        /**
         * Maximum number of persons allowed per booking.
         * Only populated when pricingType = "PER_PERSON".
         * Null means no upper limit.
         */
        private Integer maxPersonAllowed;

        /** Slots available (or unavailable) for THIS specific resource. */
        private List<SlotDto> slots;
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A single time-slot entry for a specific resource.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SlotDto {

        /**
         * Encrypted payload containing booking intent for this exact resource + slot.
         * Pass this value as an element of {@code slotKeys} when calling POST /book.
         * Null when the slot is unavailable.
         */
        private String slotKey;

        /** Deterministic hash used as a stable UI key (idempotency hint). */
        private String slotGroupId;

        /** Slot start time, formatted as "HH:mm" (e.g. "09:00"). */
        private String startTime;

        /** Slot end time,   formatted as "HH:mm" (e.g. "10:00"). */
        private String endTime;

        /** Duration of this slot in minutes. */
        private Integer durationMinutes;

        /** Price to display to the user for this slot. */
        private Double displayPrice;

        /**
         * Whether this slot is currently bookable for this resource.
         * false = already booked or disabled.
         */
        private Boolean available;
    }
}

