package com.hitendra.turf_booking_backend.dto.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hitendra.turf_booking_backend.entity.SlotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for comprehensive slot analysis for a service.
 * Shows all resources and their slots with status for admin analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceSlotsAnalysisDto {

    private Long serviceId;
    private String serviceName;
    private LocalDate analysisDate;
    private Integer totalResources;
    private Integer totalSlots;
    private Integer availableSlots;
    private Integer bookedSlots;
    private Integer disabledSlots;

    private List<ResourceSlotsDto> resources;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ResourceSlotsDto {
        private Long resourceId;
        private String resourceName;
        private Boolean resourceEnabled;
        private String openingTime;
        private String closingTime;
        private Integer slotDurationMinutes;
        private Double basePrice;
        private Integer totalSlots;
        private Integer availableSlots;
        private Integer bookedSlots;
        private Integer disabledSlots;
        private List<SlotDetailDto> slots;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SlotDetailDto {
        private String slotId;
        private String startTime;
        private String endTime;
        private String displayName;
        private Integer durationMinutes;
        private Double basePrice;
        private Double totalPrice;
        private SlotStatus status;
        private String statusReason;
        private Boolean hasAppliedPriceRules;
        private List<String> tags;
    }
}
