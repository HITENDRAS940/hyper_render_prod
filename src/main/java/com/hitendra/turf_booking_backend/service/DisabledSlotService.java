package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.slot.BulkDisableSlotRequestDto;
import com.hitendra.turf_booking_backend.dto.slot.DisableSlotRequestDto;
import com.hitendra.turf_booking_backend.dto.slot.DisabledSlotDto;
import com.hitendra.turf_booking_backend.dto.slot.GeneratedSlot;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.exception.BookingException;
import com.hitendra.turf_booking_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing disabled slots.
 * Provides functionality to disable individual slots, time ranges, or bulk disable slots.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DisabledSlotService {

    private final DisabledSlotRepository disabledSlotRepository;
    private final ServiceResourceRepository resourceRepository;
    private final ResourceSlotConfigRepository slotConfigRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final SlotGeneratorService slotGeneratorService;
    private final BookingRepository bookingRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    /**
     * Disable a single slot or time range for a resource on a specific date.
     *
     * @param request Disable slot request
     * @param adminProfileId Admin who is disabling the slot
     * @return List of disabled slot DTOs (can be multiple if time range spans multiple slots)
     */
    @Transactional
    public List<DisabledSlotDto> disableSlot(DisableSlotRequestDto request, Long adminProfileId) {
        log.info("Disabling slot for resource {} on {} from {} to {}",
                request.getResourceId(), request.getDate(), request.getStartTime(), request.getEndTime());

        // Validate resource exists
        ServiceResource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new BookingException("Resource not found with ID: " + request.getResourceId()));

        // Get admin profile
        AdminProfile adminProfile = adminProfileRepository.findById(adminProfileId)
                .orElseThrow(() -> new BookingException("Admin profile not found"));

        // Get slot configuration to determine slot boundaries
        ResourceSlotConfig config = slotConfigRepository.findByResourceId(request.getResourceId())
                .orElseThrow(() -> new BookingException("Slot configuration not found for resource"));

        // Generate slots to validate the time range
        List<GeneratedSlot> generatedSlots = slotGeneratorService.generateSlots(config);

        LocalTime endTime = request.getEndTime();
        if (endTime == null) {
            // If no end time provided, find the next slot boundary
            GeneratedSlot matchingSlot = generatedSlots.stream()
                    .filter(s -> s.getStartTime().equals(request.getStartTime()))
                    .findFirst()
                    .orElseThrow(() -> new BookingException("Invalid start time. No slot found at " + request.getStartTime()));
            endTime = matchingSlot.getEndTime();
        }

        final LocalTime finalEndTime = endTime; // Make effectively final for lambda

        // Find all slots that fall within the requested time range
        List<GeneratedSlot> slotsToDisable = generatedSlots.stream()
                .filter(slot -> !slot.getStartTime().isBefore(request.getStartTime()) &&
                              (slot.getEndTime().isBefore(finalEndTime) || slot.getEndTime().equals(finalEndTime)))
                .toList();

        if (slotsToDisable.isEmpty()) {
            throw new BookingException("No slots found in the specified time range");
        }

        // Check if any of these slots already have bookings
        List<Booking> existingBookings = bookingRepository.findByResourceIdAndBookingDate(
                request.getResourceId(),
                request.getDate()
        );

        // Filter bookings that overlap with our time range
        List<Booking> confirmedBookings = existingBookings.stream()
                .filter(b -> (b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED) &&
                           b.getStartTime().isBefore(finalEndTime) && b.getEndTime().isAfter(request.getStartTime()))
                .toList();

        if (!confirmedBookings.isEmpty()) {
            throw new BookingException("Cannot disable slot(s) with existing confirmed bookings. " +
                    "Please cancel the bookings first. Found " + confirmedBookings.size() + " confirmed booking(s).");
        }

        // Create disabled slot entries
        List<DisabledSlot> disabledSlots = new ArrayList<>();
        for (GeneratedSlot slot : slotsToDisable) {
            // Check if already disabled
            if (!disabledSlotRepository.existsByResourceIdAndStartTimeAndDisabledDate(
                    request.getResourceId(), slot.getStartTime(), request.getDate())) {

                DisabledSlot disabledSlot = DisabledSlot.builder()
                        .resource(resource)
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .disabledDate(request.getDate())
                        .reason(request.getReason())
                        .disabledBy(adminProfile)
                        .createdAt(Instant.now())
                        .build();

                disabledSlots.add(disabledSlotRepository.save(disabledSlot));
            }
        }

        log.info("Disabled {} slot(s) for resource {}", disabledSlots.size(), resource.getName());

        return disabledSlots.stream()
                .map(this::convertToDto)
                .toList();
    }

    /**
     * Bulk disable slots for multiple resources, dates, or time ranges.
     *
     * @param request Bulk disable request
     * @param adminProfileId Admin who is disabling the slots
     * @return Number of slots disabled
     */
    @Transactional
    public int bulkDisableSlots(BulkDisableSlotRequestDto request, Long adminProfileId) {
        log.info("Bulk disabling slots - resources: {}, dates: {} to {}, time: {} to {}",
                request.getResourceIds(), request.getStartDate(), request.getEndDate(),
                request.getStartTime(), request.getEndTime());

        // Get admin profile
        AdminProfile adminProfile = adminProfileRepository.findById(adminProfileId)
                .orElseThrow(() -> new BookingException("Admin profile not found"));

        // Determine which resources to disable
        List<Long> resourceIds = request.getResourceIds();
        if (resourceIds == null || resourceIds.isEmpty()) {
            if (request.getServiceId() == null) {
                throw new BookingException("Either resourceIds or serviceId must be provided");
            }
            // Get all resources for the service
            resourceIds = resourceRepository.findByServiceId(request.getServiceId()).stream()
                    .map(ServiceResource::getId)
                    .toList();
        }

        // Determine date range
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : request.getStartDate();

        int totalDisabled = 0;

        // Iterate through each resource and each date
        for (Long resourceId : resourceIds) {
            ServiceResource resource = resourceRepository.findById(resourceId)
                    .orElseThrow(() -> new BookingException("Resource not found: " + resourceId));

            ResourceSlotConfig config = slotConfigRepository.findByResourceId(resourceId).orElse(null);
            if (config == null) {
                log.warn("No slot config for resource {}, skipping", resourceId);
                continue;
            }

            List<GeneratedSlot> allSlots = slotGeneratorService.generateSlots(config);

            // Iterate through date range
            LocalDate currentDate = request.getStartDate();
            while (!currentDate.isAfter(endDate)) {

                // Determine which slots to disable on this date
                List<GeneratedSlot> slotsToDisable;

                if (request.getStartTime() != null && request.getEndTime() != null) {
                    // Disable specific time range
                    LocalTime reqStart = request.getStartTime();
                    LocalTime reqEnd = request.getEndTime();

                    slotsToDisable = allSlots.stream()
                            .filter(slot -> !slot.getStartTime().isBefore(reqStart) &&
                                          (slot.getEndTime().isBefore(reqEnd) || slot.getEndTime().equals(reqEnd)))
                            .toList();
                } else {
                    // Disable entire day (all slots)
                    slotsToDisable = allSlots;
                }

                // Create disabled slot entries
                for (GeneratedSlot slot : slotsToDisable) {
                    if (!disabledSlotRepository.existsByResourceIdAndStartTimeAndDisabledDate(
                            resourceId, slot.getStartTime(), currentDate)) {

                        DisabledSlot disabledSlot = DisabledSlot.builder()
                                .resource(resource)
                                .startTime(slot.getStartTime())
                                .endTime(slot.getEndTime())
                                .disabledDate(currentDate)
                                .reason(request.getReason())
                                .disabledBy(adminProfile)
                                .createdAt(Instant.now())
                                .build();

                        disabledSlotRepository.save(disabledSlot);
                        totalDisabled++;
                    }
                }

                currentDate = currentDate.plusDays(1);
            }
        }

        log.info("Bulk disabled {} slot(s) total", totalDisabled);
        return totalDisabled;
    }

    /**
     * Enable a previously disabled slot.
     *
     * @param disabledSlotId ID of the disabled slot to enable
     */
    @Transactional
    public void enableSlot(Long disabledSlotId) {
        DisabledSlot disabledSlot = disabledSlotRepository.findById(disabledSlotId)
                .orElseThrow(() -> new BookingException("Disabled slot not found with ID: " + disabledSlotId));

        disabledSlotRepository.delete(disabledSlot);

        log.info("Enabled slot {} on {} for resource {}",
                disabledSlot.getStartTime(), disabledSlot.getDisabledDate(),
                disabledSlot.getResource().getName());
    }

    /**
     * Enable a specific slot by resource, date, and time.
     *
     * @param resourceId Resource ID
     * @param date Date
     * @param startTime Start time of the slot
     */
    @Transactional
    public void enableSlotByTime(Long resourceId, LocalDate date, LocalTime startTime) {
        DisabledSlot disabledSlot = disabledSlotRepository.findByResourceIdAndStartTimeAndDisabledDate(
                resourceId, startTime, date)
                .orElseThrow(() -> new BookingException("No disabled slot found for the specified time"));

        disabledSlotRepository.delete(disabledSlot);

        log.info("Enabled slot {} on {} for resource {}",
                startTime, date, resourceId);
    }

    /**
     * Get all disabled slots for a resource on a specific date.
     *
     * @param resourceId Resource ID
     * @param date Date
     * @return List of disabled slots
     */
    @Transactional(readOnly = true)
    public List<DisabledSlotDto> getDisabledSlots(Long resourceId, LocalDate date) {
        List<DisabledSlot> disabledSlots = disabledSlotRepository.findByResourceIdAndDisabledDate(resourceId, date);

        return disabledSlots.stream()
                .map(this::convertToDto)
                .toList();
    }

    /**
     * Get all disabled slots for a service on a specific date.
     *
     * @param serviceId Service ID
     * @param date Date
     * @return List of disabled slots
     */
    @Transactional(readOnly = true)
    public List<DisabledSlotDto> getDisabledSlotsByService(Long serviceId, LocalDate date) {
        List<DisabledSlot> disabledSlots = disabledSlotRepository.findByResourceServiceIdAndDisabledDate(serviceId, date);

        return disabledSlots.stream()
                .map(this::convertToDto)
                .toList();
    }

    /**
     * Get all disabled slots for a resource within a date range.
     *
     * @param resourceId Resource ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of disabled slots
     */
    @Transactional(readOnly = true)
    public List<DisabledSlotDto> getDisabledSlotsInRange(Long resourceId, LocalDate startDate, LocalDate endDate) {
        List<DisabledSlot> disabledSlots = disabledSlotRepository.findByResourceId(resourceId).stream()
                .filter(ds -> !ds.getDisabledDate().isBefore(startDate) && !ds.getDisabledDate().isAfter(endDate))
                .toList();

        return disabledSlots.stream()
                .map(this::convertToDto)
                .toList();
    }

    /**
     * Delete all disabled slots for a resource on a specific date.
     *
     * @param resourceId Resource ID
     * @param date Date
     * @return Number of slots enabled
     */
    @Transactional
    public int enableAllSlotsOnDate(Long resourceId, LocalDate date) {
        List<DisabledSlot> disabledSlots = disabledSlotRepository.findByResourceIdAndDisabledDate(resourceId, date);
        int count = disabledSlots.size();

        disabledSlotRepository.deleteAll(disabledSlots);

        log.info("Enabled {} slot(s) for resource {} on {}", count, resourceId, date);
        return count;
    }

    /**
     * Convert DisabledSlot entity to DTO.
     */
    private DisabledSlotDto convertToDto(DisabledSlot disabledSlot) {
        String displayTime = disabledSlot.getStartTime().format(TIME_FORMATTER) + " - " +
                           disabledSlot.getEndTime().format(TIME_FORMATTER);

        String adminName = disabledSlot.getDisabledBy() != null
                ? disabledSlot.getDisabledBy().getUser().getName()
                : null;

        return DisabledSlotDto.builder()
                .id(disabledSlot.getId())
                .resourceId(disabledSlot.getResource().getId())
                .resourceName(disabledSlot.getResource().getName())
                .date(disabledSlot.getDisabledDate())
                .startTime(disabledSlot.getStartTime())
                .endTime(disabledSlot.getEndTime())
                .reason(disabledSlot.getReason())
                .disabledByAdminName(adminName)
                .createdAt(disabledSlot.getCreatedAt())
                .displayTime(displayTime)
                .build();
    }
}







