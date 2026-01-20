package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.service.*;
import com.hitendra.turf_booking_backend.dto.slot.GeneratedSlot;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing resource slot configurations.
 *
 * Slots are now generated DYNAMICALLY from ResourceSlotConfig.
 * No slots are stored in the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceSlotService {

    private final ResourceSlotConfigRepository resourceSlotConfigRepository;
    private final ResourcePriceRuleRepository priceRuleRepository;
    private final ServiceResourceRepository serviceResourceRepository;
    private final BookingRepository bookingRepository;
    private final SlotGeneratorService slotGeneratorService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    // ==================== Slot Configuration ====================

    /**
     * Create or update slot configuration for a resource.
     * Slots are generated dynamically - no database storage.
     */
    @Transactional
    public ResourceSlotConfigDto createOrUpdateSlotConfig(ResourceSlotConfigRequest request) {
        ServiceResource resource = serviceResourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new RuntimeException("Resource not found: " + request.getResourceId()));

        ResourceSlotConfig config = resourceSlotConfigRepository.findByResourceId(request.getResourceId())
                .orElse(new ResourceSlotConfig());

        config.setResource(resource);
        config.setOpeningTime(request.getOpeningTime());
        config.setClosingTime(request.getClosingTime());
        config.setSlotDurationMinutes(request.getSlotDurationMinutes());
        config.setBasePrice(request.getBasePrice());
        config.setEnabled(true);

        ResourceSlotConfig saved = resourceSlotConfigRepository.save(config);

        log.info("Created/Updated slot config for resource {} with {} minute slots",
                resource.getName(), request.getSlotDurationMinutes());

        return convertToConfigDto(saved);
    }

    /**
     * Get slot configuration for a resource
     */
    @Transactional(readOnly = true)
    public ResourceSlotConfigDto getSlotConfig(Long resourceId) {
        ResourceSlotConfig config = resourceSlotConfigRepository.findByResourceId(resourceId)
                .orElseThrow(() -> new RuntimeException("Slot config not found for resource: " + resourceId));
        return convertToConfigDto(config);
    }

    /**
     * Get all dynamically generated slots for a resource
     */
    @Transactional(readOnly = true)
    public List<ResourceSlotDto> getSlots(Long resourceId) {
        ResourceSlotConfig config = resourceSlotConfigRepository.findByResourceId(resourceId)
                .orElseThrow(() -> new RuntimeException("Slot config not found for resource: " + resourceId));

        List<GeneratedSlot> generatedSlots = slotGeneratorService.generateSlots(config);
        List<ResourceSlotDto> dtos = new ArrayList<>();

        for (GeneratedSlot slot : generatedSlots) {
            ResourceSlotDto dto = ResourceSlotDto.builder()
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .displayName(slot.getDisplayName())
                    .basePrice(slot.getBasePrice())
                    .durationMinutes(slot.getDurationMinutes())
                    .enabled(config.isEnabled())
                    .build();
            dtos.add(dto);
        }

        return dtos;
    }

    /**
     * Get slot availability for a resource on a specific date
     * Returns detailed status of each slot (AVAILABLE, BOOKED, DISABLED)
     */
    @Transactional(readOnly = true)
    public ResourceAvailabilityResponseDto getSlotAvailability(Long resourceId, LocalDate date) {
        ResourceSlotConfig config = resourceSlotConfigRepository.findByResourceId(resourceId)
                .orElseThrow(() -> new RuntimeException("Slot config not found for resource: " + resourceId));

        // Generate slots dynamically
        List<GeneratedSlot> generatedSlots = slotGeneratorService.generateSlots(config);

        // Get bookings for this resource on this date
        // IMPORTANT: Only consider a slot as "booked" if:
        // - status is CONFIRMED, or
        // - status is PENDING/AWAITING_CONFIRMATION AND payment_status is IN_PROGRESS or SUCCESS
        // Slots with payment_status = NOT_STARTED are NOT considered locked
        List<Booking> bookings = bookingRepository.findByResourceIdAndBookingDate(resourceId, date).stream()
                .filter(this::isBookingLockingSlot)
                .toList();

        // Get price rules for this resource
        List<ResourcePriceRule> priceRules = priceRuleRepository.findEnabledRulesByResourceId(resourceId);

        boolean isWeekend = isWeekend(date);
        DayType dayType = isWeekend ? DayType.WEEKEND : DayType.WEEKDAY;

        List<ResourceAvailabilityResponseDto.SlotDto> slotDtos = new ArrayList<>();

        for (GeneratedSlot slot : generatedSlots) {
            // Check if slot is booked
            boolean isBooked = bookings.stream().anyMatch(b ->
                    isTimeOverlap(slot.getStartTime(), slot.getEndTime(), b.getStartTime(), b.getEndTime()));

            String status = isBooked ? "BOOKED" : "AVAILABLE";
            String reason = isBooked ? "Slot already booked" : null;

            // Calculate price with rules
            Double price = calculateSlotPrice(slot, config, priceRules, dayType);

            // Build tags
            List<String> tags = new ArrayList<>();
            if (slot.getStartTime().getHour() >= 18 || slot.getStartTime().getHour() < 6) {
                tags.add("NIGHT");
            } else {
                tags.add("DAY");
            }
            if (hasApplicablePriceRule(slot.getStartTime(), priceRules, dayType)) {
                tags.add("DYNAMIC_PRICING");
            }

            // Build price breakup
            List<ResourceAvailabilityResponseDto.PriceComponent> priceBreakup = new ArrayList<>();
            priceBreakup.add(ResourceAvailabilityResponseDto.PriceComponent.builder()
                    .label("Base Price")
                    .amount(price)
                    .build());

            ResourceAvailabilityResponseDto.SlotDto slotDto = ResourceAvailabilityResponseDto.SlotDto.builder()
                    .slotId(resourceId + "-" + date + "-" + slot.getStartTime())
                    .startTime(slot.getStartTime().format(TIME_FORMATTER))
                    .endTime(slot.getEndTime().format(TIME_FORMATTER))
                    .status(status)
                    .price(price)
                    .tags(tags)
                    .priceBreakup(priceBreakup)
                    .reason(reason)
                    .build();

            slotDtos.add(slotDto);
        }

        return ResourceAvailabilityResponseDto.builder()
                .slots(slotDtos)
                .build();
    }

    /**
     * Get detailed slots for a resource on a specific date.
     * Returns slot status (AVAILABLE, BOOKED, DISABLED) with minimal info.
     */
    @Transactional(readOnly = true)
    public List<ResourceSlotDetailDto> getDetailedSlotsByResourceAndDate(Long resourceId, LocalDate date) {
        ResourceSlotConfig config = resourceSlotConfigRepository.findByResourceId(resourceId)
                .orElseThrow(() -> new RuntimeException("Slot config not found for resource: " + resourceId));

        // Generate slots dynamically
        List<GeneratedSlot> generatedSlots = slotGeneratorService.generateSlots(config);

        // Get bookings for this resource on this date
        // IMPORTANT: Only consider a slot as "booked" if:
        // - status is CONFIRMED, or
        // - status is PENDING/AWAITING_CONFIRMATION AND payment_status is IN_PROGRESS or SUCCESS
        // Slots with payment_status = NOT_STARTED are NOT considered locked
        List<Booking> bookings = bookingRepository.findByResourceIdAndBookingDate(resourceId, date).stream()
                .filter(this::isBookingLockingSlot)
                .toList();

        // Get price rules
        List<ResourcePriceRule> priceRules = priceRuleRepository.findEnabledRulesByResourceId(resourceId);
        boolean isWeekend = isWeekend(date);
        DayType dayType = isWeekend ? DayType.WEEKEND : DayType.WEEKDAY;

        List<ResourceSlotDetailDto> details = new ArrayList<>();

        for (GeneratedSlot slot : generatedSlots) {
            boolean isBooked = bookings.stream().anyMatch(b ->
                    isTimeOverlap(slot.getStartTime(), slot.getEndTime(), b.getStartTime(), b.getEndTime()));

            SlotStatus status;
            if (!config.isEnabled()) {
                status = SlotStatus.DISABLED;
            } else if (isBooked) {
                status = SlotStatus.BOOKED;
            } else {
                status = SlotStatus.AVAILABLE;
            }

            Double price = calculateSlotPrice(slot, config, priceRules, dayType);

            ResourceSlotDetailDto detail = ResourceSlotDetailDto.builder()
                    .slotId(resourceId + "-" + date + "-" + slot.getStartTime())
                    .startTime(slot.getStartTime().format(TIME_FORMATTER))
                    .endTime(slot.getEndTime().format(TIME_FORMATTER))
                    .status(status)
                    .price(price)
                    .build();

            details.add(detail);
        }

        return details;
    }

    // ==================== Helper Methods ====================

    private Double calculateSlotPrice(GeneratedSlot slot, ResourceSlotConfig config,
                                       List<ResourcePriceRule> priceRules, DayType dayType) {
        Double price = config.getBasePrice();

        // Find applicable rules
        List<ResourcePriceRule> applicableRules = priceRules.stream()
                .filter(ResourcePriceRule::isEnabled)
                .filter(r -> r.getDayType() == DayType.ALL || r.getDayType() == dayType)
                .filter(r -> isTimeInRange(slot.getStartTime(), r.getStartTime(), r.getEndTime()))
                .sorted((a, b) -> b.getPriority().compareTo(a.getPriority()))
                .toList();

        // Apply highest priority base price override
        if (!applicableRules.isEmpty() && applicableRules.get(0).getBasePrice() != null) {
            price = applicableRules.get(0).getBasePrice();
        }

        // Add extra charges from all applicable rules
        for (ResourcePriceRule rule : applicableRules) {
            if (rule.getExtraCharge() != null && rule.getExtraCharge() > 0) {
                price += rule.getExtraCharge();
            }
        }

        return Math.round(price * 100.0) / 100.0;
    }

    private boolean hasApplicablePriceRule(LocalTime time, List<ResourcePriceRule> rules, DayType dayType) {
        return rules.stream()
                .filter(ResourcePriceRule::isEnabled)
                .filter(r -> r.getDayType() == DayType.ALL || r.getDayType() == dayType)
                .anyMatch(r -> isTimeInRange(time, r.getStartTime(), r.getEndTime()));
    }

    private boolean isTimeInRange(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && time.isBefore(end);
    }

    private boolean isTimeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private ResourceSlotConfigDto convertToConfigDto(ResourceSlotConfig config) {
        int slotCount = config.getSlotCount();

        return ResourceSlotConfigDto.builder()
                .id(config.getId())
                .resourceId(config.getResource().getId())
                .resourceName(config.getResource().getName())
                .openingTime(config.getOpeningTime())
                .closingTime(config.getClosingTime())
                .slotDurationMinutes(config.getSlotDurationMinutes())
                .basePrice(config.getBasePrice())
                .enabled(config.isEnabled())
                .totalSlots(slotCount)
                .build();
    }

    /**
     * Determine if a booking is actually locking a slot (making it unavailable).
     *
     * RULE: A booking locks a slot if:
     * - status is CONFIRMED, or
     * - status is PENDING/AWAITING_CONFIRMATION AND payment_status is IN_PROGRESS or SUCCESS
     *
     * Bookings with payment_status = NOT_STARTED are NOT considered locking the slot.
     * This allows other users to book "abandoned" slots where User A created a booking
     * but never initiated payment.
     *
     * @param booking The booking to check
     * @return true if the booking is locking the slot, false otherwise
     */
    private boolean isBookingLockingSlot(Booking booking) {
        BookingStatus status = booking.getStatus();
        PaymentStatus paymentStatus = booking.getPaymentStatusEnum();

        // Confirmed bookings always lock the slot
        if (status == BookingStatus.CONFIRMED) {
            return true;
        }

        // Pending or Awaiting Confirmation bookings lock the slot ONLY if payment is in progress or succeeded
        if (status == BookingStatus.PENDING || status == BookingStatus.AWAITING_CONFIRMATION) {
            return paymentStatus == PaymentStatus.IN_PROGRESS || paymentStatus == PaymentStatus.SUCCESS;
        }

        // All other statuses (CANCELLED, EXPIRED, etc.) don't lock the slot
        return false;
    }
}

