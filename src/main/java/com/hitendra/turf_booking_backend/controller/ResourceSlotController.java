package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.service.*;
import com.hitendra.turf_booking_backend.service.PricingService;
import com.hitendra.turf_booking_backend.service.ResourceSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Public controller for accessing resource slots and availability
 */
@RestController
@RequestMapping("/resources")
@RequiredArgsConstructor
@Tag(name = "Resource Slots", description = "APIs for accessing resource slots and availability")
public class ResourceSlotController {

    private final ResourceSlotService resourceSlotService;
    private final PricingService pricingService;

    // Commented out - getEnabledSlotsByResourceId doesn't exist in ResourceSlotService
    // Use getSlotAvailability or getDetailedSlotAvailability instead
    /*
    @GetMapping("/{resourceId}/slots")
    @Operation(summary = "Get all slots for a resource", description = "Get all enabled slots for a specific resource")
    public ResponseEntity<List<ResourceSlotDto>> getResourceSlots(@PathVariable Long resourceId) {
        List<ResourceSlotDto> slots = resourceSlotService.getEnabledSlotsByResourceId(resourceId);
        return ResponseEntity.ok(slots);
    }
    */

    @GetMapping("/{resourceId}/availability")
    @Operation(summary = "Get slot availability for a date",
               description = "Get availability status and pricing for all slots of a resource on a specific date")
    public ResponseEntity<ResourceAvailabilityResponseDto> getSlotAvailability(
            @PathVariable Long resourceId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        ResourceAvailabilityResponseDto availability = resourceSlotService.getSlotAvailability(resourceId, date);
        return ResponseEntity.ok(availability);
    }

    @GetMapping("/{resourceId}/availability/detailed")
    @Operation(summary = "Get detailed slot availability for a date",
               description = "Get detailed availability status and pricing for all slots of a resource on a specific date")
    public ResponseEntity<ResourceAvailabilityResponseDto> getDetailedSlotAvailability(
            @PathVariable Long resourceId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        ResourceAvailabilityResponseDto availability = resourceSlotService.getSlotAvailability(resourceId, date);
        return ResponseEntity.ok(availability);
    }

    // Commented out - getSlotById, calculateSlotPrice, getSlotPriceBreakdown, calculatePriceBreakdown, getEnabledPriceRules
    // don't exist in the current services. Use resource-level methods instead.
    /*
    @GetMapping("/slots/{slotId}")
    @Operation(summary = "Get slot details", description = "Get details of a specific slot")
    public ResponseEntity<ResourceSlotDto> getSlotById(@PathVariable Long slotId) {
        ResourceSlotDto slot = resourceSlotService.getSlotById(slotId);
        return ResponseEntity.ok(slot);
    }

    @GetMapping("/slots/{slotId}/price")
    @Operation(summary = "Get slot price for a date",
               description = "Calculate the price for a specific slot on a specific date (includes price rules)")
    public ResponseEntity<Double> getSlotPrice(
            @PathVariable Long slotId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Double price = pricingService.calculateSlotPrice(slotId, date);
        return ResponseEntity.ok(price);
    }

    @GetMapping("/slots/{slotId}/price-breakdown")
    @Operation(summary = "Get slot price breakdown",
               description = "Get detailed price breakdown for a slot including taxes and fees")
    public ResponseEntity<PriceBreakdownDto> getSlotPriceBreakdown(
            @PathVariable Long slotId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        PriceBreakdownDto breakdown = pricingService.getSlotPriceBreakdown(slotId, date);
        return ResponseEntity.ok(breakdown);
    }

    @PostMapping("/{resourceId}/price-breakdown")
    @Operation(summary = "Get price breakdown for multiple slots",
               description = "Get detailed price breakdown for selected slots including taxes, fees, and applied rules")
    public ResponseEntity<PriceBreakdownDto> getPriceBreakdown(
            @PathVariable Long resourceId,
            @RequestParam List<Long> slotIds,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        PriceBreakdownDto breakdown = pricingService.calculatePriceBreakdown(resourceId, slotIds, date);
        return ResponseEntity.ok(breakdown);
    }
    */

    @GetMapping("/{resourceId}/config")
    @Operation(summary = "Get slot configuration", description = "Get the slot configuration for a resource")
    public ResponseEntity<ResourceSlotConfigDto> getSlotConfig(@PathVariable Long resourceId) {
        ResourceSlotConfigDto config = resourceSlotService.getSlotConfig(resourceId);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/{resourceId}/price-rules")
    @Operation(summary = "Get enabled price rules", description = "Get all enabled pricing rules for a resource")
    public ResponseEntity<List<ResourcePriceRuleDto>> getPriceRules(@PathVariable Long resourceId) {
        List<ResourcePriceRuleDto> rules = pricingService.getPriceRulesForResource(resourceId);
        return ResponseEntity.ok(rules);
    }
}
