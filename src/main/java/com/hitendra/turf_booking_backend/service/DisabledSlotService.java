package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.slot.BulkDisableSlotRequestDto;
import com.hitendra.turf_booking_backend.dto.slot.DisableSlotRequestDto;
import com.hitendra.turf_booking_backend.dto.slot.DisabledSlotDto;
import com.hitendra.turf_booking_backend.dto.slot.GeneratedSlot;
import com.hitendra.turf_booking_backend.dto.slot.UnifiedDisableSlotRequestDto;
import com.hitendra.turf_booking_backend.dto.slot.UnifiedDisableSlotResponseDto;
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
     * Unified method to disable slots - handles all scenarios:
     * - Single slot
     * - Time range (single day)
     * - Entire day(s)
     * - Multiple resources
     * - Multiple dates
     * - Service-wide disable
     *
     * @param request Unified disable request
     * @param adminProfileId Admin who is disabling the slots
     * @return Response with count and details of disabled slots
     */
    @Transactional
    public UnifiedDisableSlotResponseDto disableSlots(UnifiedDisableSlotRequestDto request, Long adminProfileId) {
        log.info("Disabling slots - resources: {}, serviceId: {}, dates: {} to {}, time: {} to {}",
                request.getResourceIds(), request.getServiceId(),
                request.getStartDate(), request.getEndDate(),
                request.getStartTime(), request.getEndTime());

        // Validate request
        if ((request.getResourceIds() == null || request.getResourceIds().isEmpty()) && request.getServiceId() == null) {
            throw new BookingException("Either resourceIds or serviceId must be provided");
        }

        // Get admin profile
        AdminProfile adminProfile = adminProfileRepository.findById(adminProfileId)
                .orElseThrow(() -> new BookingException("Admin profile not found"));

        // Determine which resources to disable
        List<Long> resourceIds = request.getResourceIds();
        if (resourceIds == null || resourceIds.isEmpty()) {
            // Get all resources for the service
            resourceIds = resourceRepository.findByServiceId(request.getServiceId()).stream()
                    .map(ServiceResource::getId)
                    .toList();

            if (resourceIds.isEmpty()) {
                throw new BookingException("No resources found for service ID: " + request.getServiceId());
            }
        }

        // Determine date range
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : request.getStartDate();

        List<DisabledSlotDto> allDisabledSlots = new ArrayList<>();
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

                if (request.getStartTime() != null) {
                    LocalTime reqStart = request.getStartTime();
                    LocalTime reqEnd = request.getEndTime();

                    if (reqEnd == null) {
                        // Single slot - find the slot that starts at this time
                        GeneratedSlot matchingSlot = allSlots.stream()
                                .filter(s -> s.getStartTime().equals(reqStart))
                                .findFirst()
                                .orElseThrow(() -> new BookingException(
                                        "Invalid start time. No slot found at " + reqStart + " for resource " + resourceId));

                        slotsToDisable = List.of(matchingSlot);
                    } else {
                        // Time range - disable all slots in the range
                        slotsToDisable = allSlots.stream()
                                .filter(slot -> !slot.getStartTime().isBefore(reqStart) &&
                                              (slot.getEndTime().isBefore(reqEnd) || slot.getEndTime().equals(reqEnd)))
                                .toList();

                        if (slotsToDisable.isEmpty()) {
                            log.warn("No slots found in time range {} to {} for resource {} on {}",
                                    reqStart, reqEnd, resourceId, currentDate);
                        }
                    }
                } else {
                    // No time specified - disable entire day (all slots)
                    slotsToDisable = allSlots;
                }

                // Check for existing confirmed bookings before disabling
                if (!slotsToDisable.isEmpty()) {
                    LocalTime firstStart = slotsToDisable.get(0).getStartTime();
                    LocalTime lastEnd = slotsToDisable.get(slotsToDisable.size() - 1).getEndTime();

                    List<Booking> existingBookings = bookingRepository.findByResourceIdAndBookingDate(
                            resourceId, currentDate);

                    List<Booking> confirmedBookings = existingBookings.stream()
                            .filter(b -> (b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.COMPLETED) &&
                                       b.getStartTime().isBefore(lastEnd) && b.getEndTime().isAfter(firstStart))
                            .toList();

                    if (!confirmedBookings.isEmpty()) {
                        log.warn("Skipping {} slots on {} for resource {} due to {} confirmed booking(s)",
                                slotsToDisable.size(), currentDate, resourceId, confirmedBookings.size());
                        currentDate = currentDate.plusDays(1);
                        continue;
                    }
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

                        DisabledSlot saved = disabledSlotRepository.save(disabledSlot);
                        allDisabledSlots.add(convertToDto(saved));
                        totalDisabled++;
                    }
                }

                currentDate = currentDate.plusDays(1);
            }
        }

        log.info("Disabled {} slot(s) total", totalDisabled);

        // Generate summary message
        String message = generateSummaryMessage(request, resourceIds.size(), totalDisabled);

        return UnifiedDisableSlotResponseDto.builder()
                .totalDisabledCount(totalDisabled)
                .disabledSlots(allDisabledSlots)
                .message(message)
                .build();
    }

    /**
     * Generate a human-readable summary message for the disable operation.
     */
    private String generateSummaryMessage(UnifiedDisableSlotRequestDto request, int resourceCount, int totalDisabled) {
        StringBuilder message = new StringBuilder();
        message.append("Successfully disabled ").append(totalDisabled).append(" slot(s)");

        if (resourceCount > 1) {
            message.append(" across ").append(resourceCount).append(" resource(s)");
        }

        if (request.getEndDate() != null && !request.getEndDate().equals(request.getStartDate())) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
            message.append(" over ").append(days).append(" day(s)");
        }

        if (request.getStartTime() != null) {
            if (request.getEndTime() != null) {
                message.append(" (").append(request.getStartTime()).append(" to ").append(request.getEndTime()).append(")");
            } else {
                message.append(" (starting at ").append(request.getStartTime()).append(")");
            }
        }

        if (request.getReason() != null && !request.getReason().isEmpty()) {
            message.append(". Reason: ").append(request.getReason());
        }

        return message.toString();
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
     * Get all disabled slots for all services managed by an admin.
     * Optionally filter by date range.
     *
     * @param adminProfileId Admin profile ID
     * @param startDate Start date (optional - defaults to today)
     * @param endDate End date (optional - defaults to far future)
     * @return List of disabled slots
     */
    @Transactional(readOnly = true)
    public List<DisabledSlotDto> getAllDisabledSlotsForAdmin(Long adminProfileId, LocalDate startDate, LocalDate endDate) {
        // Default to today if no start date provided
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now();
        // Default to far future if no end date provided (e.g., 1 year from now)
        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now().plusYears(1);

        log.info("Fetching disabled slots for admin {} from {} to {}",
                adminProfileId, effectiveStartDate, effectiveEndDate);

        // Get all services for this admin
        List<Long> serviceIds = resourceRepository.findAll().stream()
                .filter(r -> r.getService().getCreatedBy().getId().equals(adminProfileId))
                .map(r -> r.getService().getId())
                .distinct()
                .toList();

        if (serviceIds.isEmpty()) {
            log.info("No services found for admin {}", adminProfileId);
            return List.of();
        }

        // Get all disabled slots for these services within the date range
        List<DisabledSlot> disabledSlots = new ArrayList<>();
        for (Long serviceId : serviceIds) {
            List<DisabledSlot> serviceSlots = disabledSlotRepository.findByServiceIdAndDateRange(
                    serviceId, effectiveStartDate, effectiveEndDate);
            disabledSlots.addAll(serviceSlots);
        }

        log.info("Found {} disabled slot(s) for admin {}", disabledSlots.size(), adminProfileId);

        return disabledSlots.stream()
                .sorted((a, b) -> {
                    // Sort by date first, then by start time
                    int dateCompare = a.getDisabledDate().compareTo(b.getDisabledDate());
                    if (dateCompare != 0) return dateCompare;
                    return a.getStartTime().compareTo(b.getStartTime());
                })
                .map(this::convertToDto)
                .toList();
    }

    /**
     * Delete specific disabled slots by their IDs.
     * Only allows deletion if the admin owns the service.
     *
     * @param disabledSlotIds List of disabled slot IDs to delete
     * @param adminProfileId Admin profile ID requesting the deletion
     * @return Number of slots successfully deleted
     */
    @Transactional
    public int deleteDisabledSlotsByIds(List<Long> disabledSlotIds, Long adminProfileId) {
        if (disabledSlotIds == null || disabledSlotIds.isEmpty()) {
            return 0;
        }

        log.info("Admin {} attempting to delete {} disabled slot(s): {}",
                adminProfileId, disabledSlotIds.size(), disabledSlotIds);

        int deletedCount = 0;

        for (Long disabledSlotId : disabledSlotIds) {
            try {
                DisabledSlot disabledSlot = disabledSlotRepository.findById(disabledSlotId).orElse(null);

                if (disabledSlot == null) {
                    log.warn("Disabled slot {} not found, skipping", disabledSlotId);
                    continue;
                }

                // Verify that the admin owns the service
                Long serviceOwnerId = disabledSlot.getResource().getService().getCreatedBy().getId();
                if (!serviceOwnerId.equals(adminProfileId)) {
                    log.warn("Admin {} does not own service for disabled slot {}, skipping",
                            adminProfileId, disabledSlotId);
                    continue;
                }

                // Delete the disabled slot
                disabledSlotRepository.delete(disabledSlot);
                deletedCount++;

                log.info("Deleted disabled slot {} for resource {} on {} at {}",
                        disabledSlotId,
                        disabledSlot.getResource().getName(),
                        disabledSlot.getDisabledDate(),
                        disabledSlot.getStartTime());

            } catch (Exception e) {
                log.error("Failed to delete disabled slot {}: {}", disabledSlotId, e.getMessage());
                // Continue with next slot
            }
        }

        log.info("Successfully deleted {} out of {} disabled slot(s) for admin {}",
                deletedCount, disabledSlotIds.size(), adminProfileId);

        return deletedCount;
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







