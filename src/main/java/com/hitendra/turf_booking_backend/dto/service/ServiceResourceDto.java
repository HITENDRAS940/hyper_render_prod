package com.hitendra.turf_booking_backend.dto.service;

import com.hitendra.turf_booking_backend.entity.Activity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResourceDto {
    private Long id;
    private Long serviceId;
    private String serviceName;
    private String name;
    private String description;
    private boolean enabled;
    private List<Activity> activities;

    /** "PER_SLOT" or "PER_PERSON" */
    private String pricingType;

    /** Max persons per booking; null means no limit. Only relevant for PER_PERSON. */
    private Integer maxPersonAllowed;
}

