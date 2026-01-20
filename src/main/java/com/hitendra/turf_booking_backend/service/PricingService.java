package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.booking.PriceBreakdownDto;
import com.hitendra.turf_booking_backend.dto.service.ResourcePriceRuleDto;
import com.hitendra.turf_booking_backend.dto.service.ResourcePriceRuleRequest;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.repository.ResourcePriceRuleRepository;
import com.hitendra.turf_booking_backend.repository.ServiceResourceRepository;
import com.hitendra.turf_booking_backend.repository.ResourceSlotConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final ResourcePriceRuleRepository priceRuleRepository;
    private final ServiceResourceRepository serviceResourceRepository;
    private final ResourceSlotConfigRepository resourceSlotConfigRepository;

    // Configurable fee rates
    @Value("${pricing.platform-fee-rate:2.0}")
    private Double platformFeeRate;  // Platform fee percentage (2%)

    @Value("${pricing.platform-fee-fixed:0.0}")
    private Double platformFeeFixed;  // Fixed platform fee (not used currently)

    // ==================== Price Rule Management ====================

    /**
     * Create a new price rule
     */
    @Transactional
    public ResourcePriceRuleDto createPriceRule(ResourcePriceRuleRequest request) {
        ServiceResource resource = serviceResourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new RuntimeException("Resource not found: " + request.getResourceId()));

        ResourceSlotConfig config = resource.getSlotConfig();
        if (config == null) {
            throw new RuntimeException("Resource has no slot configuration. Configure slots first.");
        }

        ResourcePriceRule rule = ResourcePriceRule.builder()
                .resourceSlotConfig(config)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .dayType(request.getDayType())
                .basePrice(request.getBasePrice())
                .extraCharge(request.getExtraCharge())
                .reason(request.getReason())
                .priority(request.getPriority() != null ? request.getPriority() : 1)
                .enabled(true)
                .build();

        ResourcePriceRule saved = priceRuleRepository.save(rule);
        log.info("Created price rule for resource {} - {} to {} on {}",
                resource.getName(), request.getStartTime(), request.getEndTime(), request.getDayType());

        return convertToRuleDto(saved);
    }

    /**
     * Get all price rules for a resource
     */
    public List<ResourcePriceRuleDto> getPriceRulesForResource(Long resourceId) {
        return priceRuleRepository.findEnabledRulesByResourceId(resourceId).stream()
                .map(this::convertToRuleDto)
                .collect(Collectors.toList());
    }

    /**
     * Delete a price rule
     */
    @Transactional
    public void deletePriceRule(Long ruleId) {
        ResourcePriceRule rule = priceRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Price rule not found: " + ruleId));
        priceRuleRepository.delete(rule);
        log.info("Deleted price rule {}", ruleId);
    }

    /**
     * Enable/disable a price rule
     */
    @Transactional
    public ResourcePriceRuleDto togglePriceRule(Long ruleId, boolean enabled) {
        ResourcePriceRule rule = priceRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Price rule not found: " + ruleId));
        rule.setEnabled(enabled);
        ResourcePriceRule saved = priceRuleRepository.save(rule);
        log.info("Price rule {} is now {}", ruleId, enabled ? "enabled" : "disabled");
        return convertToRuleDto(saved);
    }

    // ==================== Price Calculation ====================

    /**
     * Calculate simple total price for a slot time on a date (for availability display)
     */
    public Double calculateSlotPriceForTime(Long resourceId, LocalTime startTime, LocalDate date) {
        ResourceSlotConfig config = resourceSlotConfigRepository.findByResourceId(resourceId)
                .orElseThrow(() -> new RuntimeException("Slot config not found for resource: " + resourceId));

        boolean isWeekend = isWeekend(date);
        DayType dayType = isWeekend ? DayType.WEEKEND : DayType.WEEKDAY;

        // Start with base price from config
        double basePrice = config.getBasePrice();

        // Get applicable rules
        List<ResourcePriceRule> rules = priceRuleRepository.findApplicableRules(resourceId, dayType, startTime);

        // Apply highest priority rule's base price if set
        if (!rules.isEmpty() && rules.get(0).getBasePrice() != null) {
            basePrice = rules.get(0).getBasePrice();
        }

        // Add extra charges from all matching rules
        double extraCharges = rules.stream()
                .filter(r -> r.getExtraCharge() != null)
                .mapToDouble(ResourcePriceRule::getExtraCharge)
                .sum();

        return roundToTwoDecimals(basePrice + extraCharges);
    }

    /**
     * Calculate complete price breakdown for a time range
     * ONLY includes slot subtotal + 2% platform fee (no taxes)
     */
    public PriceBreakdownDto calculatePriceBreakdownForTimeRange(Long resourceId, LocalTime startTime, LocalTime endTime, LocalDate date) {
        ServiceResource resource = serviceResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found: " + resourceId));

        // Get config for base price
        ResourceSlotConfig config = resourceSlotConfigRepository.findByResourceId(resourceId)
                .orElseThrow(() -> new RuntimeException("Slot config not found for resource: " + resourceId));

        long durationMinutes = Duration.between(startTime, endTime).toMinutes();
        // Calculate how many "slots" this duration represents
        double slotsCount = (double) durationMinutes / config.getSlotDurationMinutes();

        boolean isWeekend = isWeekend(date);
        DayType dayType = isWeekend ? DayType.WEEKEND : DayType.WEEKDAY;

        // Get all enabled price rules for this resource
        List<ResourcePriceRule> allRules = priceRuleRepository.findEnabledRulesByResourceId(resourceId);

        // Start with base price from config
        double basePrice = config.getBasePrice();

        // Find applicable price rules for this time slot
        List<ResourcePriceRule> applicableRules = findApplicableRules(allRules, dayType, startTime);

        // Apply highest priority rule if it has a base price override
        if (!applicableRules.isEmpty()) {
            ResourcePriceRule topRule = applicableRules.get(0);
            if (topRule.getBasePrice() != null) {
                basePrice = topRule.getBasePrice();
            }
        }

        // Calculate slot subtotal (base price * number of slots)
        double slotSubtotal = basePrice * slotsCount;

        // Calculate extra charges
        double totalExtraCharges = 0.0;
        List<PriceBreakdownDto.AppliedRule> appliedRules = new ArrayList<>();

        for (ResourcePriceRule rule : applicableRules) {
            if (rule.getExtraCharge() != null && rule.getExtraCharge() > 0) {
                totalExtraCharges += rule.getExtraCharge() * slotsCount;

                appliedRules.add(PriceBreakdownDto.AppliedRule.builder()
                        .ruleId(rule.getId())
                        .reason(rule.getReason())
                        .extraCharge(rule.getExtraCharge())
                        .timeRange(rule.getStartTime() + " - " + rule.getEndTime())
                        .dayType(rule.getDayType().name())
                        .build());
            }
        }

        // Calculate totals
        double subtotal = slotSubtotal + totalExtraCharges;

        // Calculate 2% platform fee only (NO TAXES)
        double platformFee = roundToTwoDecimals(subtotal * (platformFeeRate / 100));
        double totalAmount = roundToTwoDecimals(subtotal + platformFee);

        return PriceBreakdownDto.builder()
                .slotSubtotal(roundToTwoDecimals(slotSubtotal))
                .extraCharges(roundToTwoDecimals(totalExtraCharges))
                .appliedRules(appliedRules)
                .subtotal(roundToTwoDecimals(subtotal))
                .convenienceFee(platformFee)
                .convenienceFeeRate(platformFeeRate)
                .discountAmount(0.0)
                .discountCode(null)
                .discountReason(null)
                .totalAmount(totalAmount)
                .currency("INR")
                .build();
    }

    // ==================== Helper Methods ====================

    /**
     * Find applicable rules for a specific day type and time
     */
    private List<ResourcePriceRule> findApplicableRules(List<ResourcePriceRule> allRules, DayType dayType, LocalTime time) {
        return allRules.stream()
                .filter(r -> r.getDayType() == DayType.ALL || r.getDayType() == dayType)
                .filter(r -> isTimeInRange(time, r.getStartTime(), r.getEndTime()))
                .sorted((a, b) -> b.getPriority().compareTo(a.getPriority()))
                .collect(Collectors.toList());
    }

    private boolean isTimeInRange(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && time.isBefore(end);
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private ResourcePriceRuleDto convertToRuleDto(ResourcePriceRule rule) {
        return ResourcePriceRuleDto.builder()
                .id(rule.getId())
                .startTime(rule.getStartTime())
                .endTime(rule.getEndTime())
                .dayType(rule.getDayType())
                .basePrice(rule.getBasePrice())
                .extraCharge(rule.getExtraCharge())
                .reason(rule.getReason())
                .priority(rule.getPriority())
                .enabled(rule.isEnabled())
                .build();
    }

    public ResourcePriceRuleDto updateRuleById(Long ruleId, ResourcePriceRuleRequest request) {
        ResourcePriceRule rule = priceRuleRepository.findById(ruleId)
                .orElseThrow(() -> new RuntimeException("Price rule not found: " + ruleId));
        rule.setStartTime(request.getStartTime());
        rule.setEndTime(request.getEndTime());
        rule.setDayType(request.getDayType());
        rule.setBasePrice(request.getBasePrice());
        rule.setExtraCharge(request.getExtraCharge());
        rule.setReason(request.getReason());
        rule.setPriority(request.getPriority());

        return convertToRuleDto(priceRuleRepository.save(rule));
    }

}

