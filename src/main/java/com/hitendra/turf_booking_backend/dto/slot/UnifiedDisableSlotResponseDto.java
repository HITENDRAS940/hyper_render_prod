package com.hitendra.turf_booking_backend.dto.slot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for unified disable slots operation.
 * Contains both the count of disabled slots and details of each disabled slot.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedDisableSlotResponseDto {

    /**
     * Total number of slots disabled
     */
    private int totalDisabledCount;

    /**
     * List of all disabled slots with their details
     */
    private List<DisabledSlotDto> disabledSlots;

    /**
     * Summary message describing what was disabled
     */
    private String message;
}

