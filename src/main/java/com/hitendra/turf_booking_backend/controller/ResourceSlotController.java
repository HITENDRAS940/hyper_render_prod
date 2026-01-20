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
