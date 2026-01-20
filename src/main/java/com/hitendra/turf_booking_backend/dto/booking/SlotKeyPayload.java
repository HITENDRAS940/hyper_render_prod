package com.hitendra.turf_booking_backend.dto.booking;

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
}
