package com.hitendra.turf_booking_backend.dto.slot;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for deleting disabled slots by their IDs.
 * Supports both single and bulk deletion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteDisabledSlotsRequest {

    /**
     * List of disabled slot IDs to delete (enable).
     * Can contain a single ID or multiple IDs for bulk deletion.
     */
    @NotEmpty(message = "At least one disabled slot ID is required")
    private List<Long> disabledSlotIds;
}

