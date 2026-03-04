package com.hitendra.turf_booking_backend.dto.booking;

import com.hitendra.turf_booking_backend.entity.ResourceSelectionMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotKeyPayload {
    private String slotGroupId;
    private Long serviceId;
    private String activityCode;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private List<Long> resourceIds;
    private Double quotedPrice;
    private Long expiresAt;

    /**
     * Resource selection mode embedded in the payload at slot-key generation time.
     *
     * AUTO   → resourceIds contains the full pooled-resource list; backend
     *          will allocate one using its priority algorithm.
     * MANUAL → resourceIds is a single-element list containing the specific
     *          resource the user chose; backend must honour that choice.
     *
     * May be null for legacy slot keys (treated as AUTO).
     */
    private ResourceSelectionMode resourceSelectionMode;
}


