package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.slot.GeneratedSlot;
import com.hitendra.turf_booking_backend.entity.ResourceSlotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;

/**
 * Service for generating slots dynamically from ResourceSlotConfig.
 *
 * Slots are NOT stored in database - they are generated on-the-fly based on:
 * - openingTime: When the first slot starts
 * - closingTime: When the last slot ends
 * - slotDurationMinutes: Duration of each slot
 * - basePrice: Default price for all slots
 *
 * Example:
 *   openingTime = 06:00
 *   closingTime = 10:00
 *   slotDurationMinutes = 60
 *
 *   Generated slots:
 *     - 06:00 - 07:00
 *     - 07:00 - 08:00
 *     - 08:00 - 09:00
 *     - 09:00 - 10:00
 */
@Service
@Slf4j
public class SlotGeneratorService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    /**
     * Generate all slots for a resource based on its configuration.
     *
     * @param config The ResourceSlotConfig containing slot generation parameters
     * @return List of generated slots
     */
    public List<GeneratedSlot> generateSlots(ResourceSlotConfig config) {
        List<GeneratedSlot> slots = new ArrayList<>();

        if (config == null || !config.isEnabled()) {
            log.warn("Config is null or disabled");
            return slots;
        }

        LocalTime openingTime = config.getOpeningTime();
        LocalTime closingTime = config.getClosingTime();
        Integer durationMinutes = config.getSlotDurationMinutes();
        Double basePrice = config.getBasePrice();

        if (openingTime == null || closingTime == null || durationMinutes == null || durationMinutes <= 0) {
            log.warn("Invalid config parameters for resource {}",
                    config.getResource() != null ? config.getResource().getId() : "unknown");
            return slots;
        }

        int displayOrder = 0;

        // Calculate total open duration in minutes to avoid infinite loops
        long totalOpenMinutes;
        if (closingTime.isAfter(openingTime)) {
            totalOpenMinutes = Duration.between(openingTime, closingTime).toMinutes();
        } else {
            // Overnight or 24 hours (closing <= opening)
            // (24 * 60) - openingMinutes + closingMinutes
            int openingMinutes = openingTime.getHour() * 60 + openingTime.getMinute();
            int closingMinutes = closingTime.getHour() * 60 + closingTime.getMinute();
            totalOpenMinutes = (24 * 60) - openingMinutes + closingMinutes;
        }

        int currentMinuteOffset = 0;

        while (currentMinuteOffset + durationMinutes <= totalOpenMinutes) {
            LocalTime slotStart = openingTime.plusMinutes(currentMinuteOffset);
            LocalTime slotEnd = slotStart.plusMinutes(durationMinutes);

            String displayName = slotStart.format(TIME_FORMATTER) + " - " + slotEnd.format(TIME_FORMATTER);

            GeneratedSlot slot = GeneratedSlot.builder()
                    .startTime(slotStart)
                    .endTime(slotEnd)
                    .durationMinutes(durationMinutes)
                    .displayName(displayName)
                    .basePrice(basePrice)
                    .displayOrder(displayOrder)
                    .build();

            slots.add(slot);

            currentMinuteOffset += durationMinutes;
            displayOrder++;

            // Safety break to prevent massive memory usage
            if (slots.size() > 500) {
                log.warn("Too many slots generated for resource {}. Stopping at 500.", config.getId());
                break;
            }
        }

        log.debug("Generated {} slots for resource config {}", slots.size(), config.getId());

        return slots;
    }
}