package com.hitendra.turf_booking_backend.dto.service;

import com.hitendra.turf_booking_backend.entity.Activity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full detail view of a single resource — for manager use only.
 * Includes the resource's basic info, slot configuration, and ALL price rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDetailDto {

    // ── Basic info ──────────────────────────────────────────────────────────
    private Long id;
    private Long serviceId;
    private String serviceName;
    private String name;
    private String description;
    private boolean enabled;
    private String pricingType;
    private Integer maxPersonAllowed;
    private List<Activity> activities;

    // ── Slot configuration ──────────────────────────────────────────────────
    private ResourceSlotConfigDto slotConfig;

    // ── Price rules (all, enabled + disabled) ──────────────────────────────
    private List<ResourcePriceRuleDto> priceRules;
}

