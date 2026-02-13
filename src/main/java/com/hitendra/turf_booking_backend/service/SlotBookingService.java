package com.hitendra.turf_booking_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hitendra.turf_booking_backend.dto.booking.*;
import com.hitendra.turf_booking_backend.dto.slot.GeneratedSlot;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.exception.BookingException;
import com.hitendra.turf_booking_backend.repository.*;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import com.hitendra.turf_booking_backend.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for slot-based booking with resource pooling and PRIORITY-BASED ALLOCATION.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * CORE RULES:
 * ═══════════════════════════════════════════════════════════════════════════════════
 * 1. Resources with same activity AND same price are pooled together
 * 2. Frontend never sees resource IDs when resources are pooled
 * 3. Frontend receives aggregated slot availability with availableCount
 * 4. Frontend sends only intent (serviceId, activity, date, slotId)
 * 5. Backend is solely responsible for resource allocation
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * PRIORITY-BASED RESOURCE ALLOCATION:
 * ═══════════════════════════════════════════════════════════════════════════════════
 * When multiple resources support the requested activity, we allocate based on priority:
 *
 * PRIORITY 1 (HIGHEST): EXCLUSIVE resources (support ONLY the requested activity)
 *   Example: User wants Cricket → First try "Cricket Only Ground"
 *   Reason: Keeps multi-activity resources free for users who need them
 *
 * PRIORITY 2 (LOWER): MULTI-ACTIVITY resources (support requested + other activities)
 *   Example: User wants Cricket → If Cricket-only unavailable, try "Cricket + Football Ground"
 *   Reason: Fallback when exclusive resources are booked
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * EXAMPLE SCENARIO:
 * ═══════════════════════════════════════════════════════════════════════════════════
 * Service: Sports Arena
 * Resources:
 *   - Ground A: Cricket ONLY          ← EXCLUSIVE (1 activity)
 *   - Ground B: Cricket + Football    ← MULTI-ACTIVITY (2 activities)
 *
 * User Request: Activity = CRICKET, Date = Dec 25, Time = 7 AM
 *
 * Allocation Logic:
 *   Step 1: Find all resources supporting CRICKET → [Ground A, Ground B]
 *   Step 2: Categorize by activity count:
 *           - EXCLUSIVE (1 activity): [Ground A]
 *           - MULTI-ACTIVITY (2+ activities): [Ground B]
 *   Step 3: Check EXCLUSIVE resources first:
 *           - Is Ground A available at 7 AM? → If YES, assign Ground A ✓
 *   Step 4: If no EXCLUSIVE available, check MULTI-ACTIVITY:
 *           - Is Ground B available at 7 AM? → If YES, assign Ground B ✓
 *   Step 5: If none available → Return 409 Conflict
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * WHY THIS MATTERS:
 * ═══════════════════════════════════════════════════════════════════════════════════
 * - Maximizes resource utilization across all activity types
 * - Prevents scenario where Cricket user takes Cricket+Football ground when Cricket-only is free
 * - Leaves multi-activity resources available for users who genuinely need them
 * - Better overall booking experience for all users
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * BOOKING FLOW:
 * ═══════════════════════════════════════════════════════════════════════════════════
 * 1. Validate request inputs (never trust frontend)
 * 2. Check idempotency key for retry handling
 * 3. Find all compatible resources (same activity)
 * 4. Filter to identical resources (same price)
 * 5. Categorize resources by activity count (exclusive vs multi-activity)
 * 6. Lock resources with pessimistic locking
 * 7. Find available resource using PRIORITY order
 * 8. Create booking with assigned resource
 * 9. Return booking response
 */
@Service
@RequiredArgsConstructor
public class SlotBookingService {

    private static final Logger log = LoggerFactory.getLogger(SlotBookingService.class);

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceResourceRepository resourceRepository;
    private final ResourceSlotConfigRepository slotConfigRepository;
    private final ResourcePriceRuleRepository priceRuleRepository;
    private final DisabledSlotRepository disabledSlotRepository;
    private final ActivityRepository activityRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final AuthUtil authUtil;
    private final SlotGeneratorService slotGeneratorService;
    private final CryptoUtil cryptoUtil;
    private final ObjectMapper objectMapper;
    private final RefundService refundService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Value("${pricing.platform-fee-rate:2.0}")
    private Double platformFeeRate;

    @Value("${pricing.online-payment-percent:20}")
    private Double onlinePaymentPercent;

    // ==================== SLOT AVAILABILITY (Aggregated) ====================

    /**
     * Get aggregated slot availability for a service activity on a date.
     * Pools identical resources and returns availableCount per slot.
     *
     * POOLING RULES:
     * - Resources are pooled if they support the same activity AND have the same base price
     * - If prices differ, they are NOT pooled (breaks pooling)
     * - Returns aggregated slots with availableCount across pool
     *
     * ═══════════════════════════════════════════════════════════════════════════
     * POOLING RULES:
     * ═══════════════════════════════════════════════════════════════════════════
     * - Resources are pooled if they support the same activity AND have the same base price
     * - If prices differ, they are NOT pooled (breaks pooling)
     * - Returns aggregated slots with availableCount across pool
     *
     * ═══════════════════════════════════════════════════════════════════════════
     * PRIORITY-BASED BREAKDOWN:
     * ═══════════════════════════════════════════════════════════════════════════
     * The response includes a breakdown of availability by resource type:
     *
     * - exclusiveAvailableCount: Resources that ONLY support the requested activity
     * - multiActivityAvailableCount: Resources that support multiple activities
     *
     * This helps users understand that their booking will prefer exclusive resources
     * (when available) to keep multi-activity resources free for other users.
     *
     * @param serviceId The service ID
     * @param activityCode The activity code (e.g., "FOOTBALL")
     * @param date The date to check availability
     * @return SlotAvailabilityResponseDto with aggregated slots
     */
    @Transactional(readOnly = true)
    public SlotAvailabilityResponseDto getAggregatedSlotAvailability (
            Long serviceId, String activityCode, LocalDate date
    ) {

        // ═══════════════════════════════════════════════════════════════════════════
        // STEP 1: VALIDATE SERVICE AND ACTIVITY
        // ═══════════════════════════════════════════════════════════════════════════

        // Validate service exists and is available
        com.hitendra.turf_booking_backend.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new BookingException("Service not found: " + serviceId));

        if (!service.isAvailability()) {
            throw new BookingException("Service is currently unavailable");
        }

        // Validate activity exists and is enabled
        Activity activity = activityRepository.findByCode(activityCode)
                .orElseThrow(() -> new BookingException("Activity not found: " + activityCode));

        if (!activity.isEnabled()) {
            throw new BookingException("Activity is currently unavailable: " + activityCode);
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // STEP 2: FIND COMPATIBLE RESOURCES
        // ═══════════════════════════════════════════════════════════════════════════
        // Find all resources that:
        // - Belong to this service
        // - Support the requested activity
        // - Are enabled

        List<ServiceResource> compatibleResources = resourceRepository
                .findByServiceIdAndActivityCode(serviceId, activityCode);

        if (compatibleResources.isEmpty()) {
            throw new BookingException("No resources available for activity: " + activityCode);
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // STEP 3: GROUP BY PRICE FOR POOLING
        // ═══════════════════════════════════════════════════════════════════════════
        // Resources with same base price are pooled together
        // This ensures fair pricing for all slots in the pool

        Map<Double, List<ServiceResource>> resourcesByPrice = groupResourcesByPrice(compatibleResources);

        // Use the largest pool (most common price)
        Double poolPrice = resourcesByPrice.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().size()))
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new BookingException("No valid resource pool found"));

        List<ServiceResource> pooledResources = resourcesByPrice.get(poolPrice);

        // ═══════════════════════════════════════════════════════════════════════════
        // STEP 4: CATEGORIZE RESOURCES BY PRIORITY
        // ═══════════════════════════════════════════════════════════════════════════
        // Split resources into:
        // - EXCLUSIVE: Support only the requested activity (priority 1)
        // - MULTI-ACTIVITY: Support multiple activities (priority 2)

        CategorizedResources categorized = categorizeResourcesByPriority(pooledResources, activityCode);

        log.info("Pooling {} resources for activity '{}': {} exclusive, {} multi-activity",
                pooledResources.size(), activityCode,
                categorized.exclusiveResources().size(),
                categorized.multiActivityResources().size());

        // ═══════════════════════════════════════════════════════════════════════════
        // STEP 5: GET SLOT CONFIGURATION AND GENERATE SLOTS DYNAMICALLY
        // ═══════════════════════════════════════════════════════════════════════════

        ServiceResource primaryResource = pooledResources.get(0);
        ResourceSlotConfig slotConfig = slotConfigRepository.findByResourceId(primaryResource.getId())
                .orElseThrow(() -> new BookingException("Slot configuration not found"));

        // Generate slots dynamically from config (NOT from database)
        List<GeneratedSlot> slots = slotGeneratorService.generateSlots(slotConfig);

        if (slots.isEmpty()) {
            throw new BookingException("No slots configured for this resource");
        }

        // FILTER SLOTS BASED ON CURRENT TIME (IST)
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        ZonedDateTime nowIST = ZonedDateTime.now(istZone);
        LocalDate todayIST = nowIST.toLocalDate();
        LocalTime timeIST = nowIST.toLocalTime();

        if (date.isBefore(todayIST)) {
            // If date is in the past, return empty list
            return SlotAvailabilityResponseDto.builder().slots(Collections.emptyList()).build();
        }

        if (date.equals(todayIST)) {
            slots = slots.stream()
                    .filter(slot -> slot.getEndTime().isAfter(timeIST))
                    .collect(Collectors.toList());
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // STEP 6: GET ACTIVE BOOKINGS
        // ═══════════════════════════════════════════════════════════════════════════

        List<Long> resourceIds = pooledResources.stream()
                .map(ServiceResource::getId)
                .toList();

        List<Booking> activeBookings = bookingRepository.findActiveBookingsForResources(resourceIds, date);

        // ═══════════════════════════════════════════════════════════════════════════
        // STEP 7: BUILD AGGREGATED SLOT AVAILABILITY
        // ═══════════════════════════════════════════════════════════════════════════

        boolean isWeekend = isWeekend(date);
        List<ResourcePriceRule> priceRules = priceRuleRepository.findEnabledRulesByResourceId(primaryResource.getId());

        List<SlotAvailabilityResponseDto.SlotDto> aggregatedSlots = new ArrayList<>();

        // Collect and sort resource IDs for the key (Moved outside loop for performance)
        List<Long> sortedResourceIds = pooledResources.stream()
                .map(ServiceResource::getId)
                .sorted()
                .toList();

        for (GeneratedSlot slot : slots) {
            // ───────────────────────────────────────────────────────────────────────
            // Count TOTAL booked resources for this slot
            // ───────────────────────────────────────────────────────────────────────
            int totalBookedCount = countBookedResources(activeBookings, slot.getStartTime(), slot.getEndTime());
            int totalAvailableCount = pooledResources.size() - totalBookedCount;

            // ───────────────────────────────────────────────────────────────────────
            // Calculate pricing using dynamic price rules
            // ───────────────────────────────────────────────────────────────────────
            Double slotPrice = calculateDynamicSlotPrice(slot, priceRules, isWeekend, slotConfig);


            // ───────────────────────────────────────────────────────────────────────
            // Build the aggregated slot DTO (Simplified)
            // ───────────────────────────────────────────────────────────────────────

            // Generate Slot Group ID (Deterministic Hash)
            // serviceId + date + startTime + endTime + sorted(resourceIds)
            String hashInput = serviceId + "-" + date + "-" + slot.getStartTime() + "-" + slot.getEndTime() + "-" + sortedResourceIds;
            String slotGroupId = cryptoUtil.generateHash(hashInput);

            // Create payload
            SlotKeyPayload payload = SlotKeyPayload.builder()
                    .slotGroupId(slotGroupId)
                    .serviceId(serviceId)
                    .activityCode(activityCode)
                    .date(date)
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .resourceIds(sortedResourceIds)
                    .quotedPrice(slotPrice)
                    .expiresAt(Instant.now().plusSeconds(600).getEpochSecond()) // 10 mins expiry
                    .build();

            // Encrypt payload to get Slot Key
            String slotKey = null;
            if (totalAvailableCount > 0) {
                try {
                    String jsonPayload = objectMapper.writeValueAsString(payload);
                    slotKey = cryptoUtil.encrypt(jsonPayload);
                } catch (Exception e) {
                    log.error("Error generating slot key", e);
                    // Don't throw, just leave slotKey null
                }
            }

            SlotAvailabilityResponseDto.SlotDto aggregatedSlot = SlotAvailabilityResponseDto.SlotDto.builder()
                    .slotKey(slotKey)
                    .slotGroupId(slotGroupId)
                    .startTime(slot.getStartTime().format(TIME_FORMATTER))
                    .endTime(slot.getEndTime().format(TIME_FORMATTER))
                    .durationMinutes(slot.getDurationMinutes())
                    .displayPrice(slotPrice)
                    .availableCount(Math.max(0, totalAvailableCount))
                    .totalCount(pooledResources.size())
                    .available(totalAvailableCount > 0)
                    .build();

            aggregatedSlots.add(aggregatedSlot);
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // BUILD FINAL RESPONSE
        // ═══════════════════════════════════════════════════════════════════════════

        return SlotAvailabilityResponseDto.builder()
                .slots(aggregatedSlots)
                .build();
    }

    // ==================== SLOT BOOKING (Intent-based) ====================

    /**
     * Book multiple slots as a SINGLE booking.
     * Merges contiguous slots into one booking entity.
     *
     * BOOKING FLOW:
     * 1. Decrypt and validate all slot keys
     * 2. Ensure slots are contiguous and for same service/date
     * 3. Calculate total price
     * 4. Find ONE resource available for the FULL duration
     * 5. Create ONE booking entity
     *
     * @param request The booking request with multiple slot keys
     * @return BookingResponseDto with merged booking details
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingResponseDto createSlotBooking(SlotBookingRequestDto request) {

        log.info("Processing slot booking request with {} slotKeys, paymentMethod={}",
                request.getSlotKeys() != null ? request.getSlotKeys().size() : 0,
                request.getPaymentMethod());

        if (request.getSlotKeys() == null || request.getSlotKeys().isEmpty()) {
            throw new BookingException("At least one slot must be selected");
        }

        // STEP 1: Check idempotency key
        if (request.getIdempotencyKey() != null && !request.getIdempotencyKey().isBlank()) {
            Optional<Booking> existingBooking = bookingRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existingBooking.isPresent()) {
                log.info("Idempotent request detected. Returning existing booking: {}",
                        existingBooking.get().getReference());
                return convertToResponseDto(existingBooking.get());
            }
        }

        // STEP 2: Decrypt and Validate all slot keys
        List<SlotKeyPayload> payloads = new ArrayList<>();
        for (String slotKey : request.getSlotKeys()) {
            try {
                String jsonPayload = cryptoUtil.decrypt(slotKey);
                SlotKeyPayload payload = objectMapper.readValue(jsonPayload, SlotKeyPayload.class);

                if (payload.getExpiresAt() < Instant.now().getEpochSecond()) {
                    throw new BookingException("One or more slot keys have expired. Please refresh availability.");
                }
                payloads.add(payload);
            } catch (Exception e) {
                log.error("Error decrypting slot key", e);
                throw new BookingException("Invalid or expired slot key");
            }
        }

        // STEP 3: Validate Consistency (Service, Activity, Date)
        SlotKeyPayload firstPayload = payloads.get(0);
        Long serviceId = firstPayload.getServiceId();
        String activityCode = firstPayload.getActivityCode();
        LocalDate bookingDate = firstPayload.getDate();
        List<Long> pooledResourceIds = firstPayload.getResourceIds();

        for (SlotKeyPayload payload : payloads) {
            if (!payload.getServiceId().equals(serviceId) ||
                !payload.getActivityCode().equals(activityCode) ||
                !payload.getDate().equals(bookingDate)) {
                throw new BookingException("All slots must be for the same service, activity, and date");
            }
        }

        // STEP 4: Determine Time Range and Contiguity
        payloads.sort(Comparator.comparing(SlotKeyPayload::getStartTime));

        LocalTime startTime = payloads.get(0).getStartTime();
        LocalTime endTime = payloads.get(payloads.size() - 1).getEndTime();

        // Check contiguity
        for (int i = 0; i < payloads.size() - 1; i++) {
            if (!payloads.get(i).getEndTime().equals(payloads.get(i+1).getStartTime())) {
                throw new BookingException("Selected slots must be contiguous for a single booking");
            }
        }

        log.info("Booking intent: serviceId={}, activity={}, date={}, time={}-{}, paymentMethod={}",
                serviceId, activityCode, bookingDate, startTime, endTime, request.getPaymentMethod());

        // STEP 5: Validate service
        com.hitendra.turf_booking_backend.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new BookingException("Service not found: " + serviceId));

        if (!service.isAvailability()) {
            throw new BookingException("Service is currently unavailable");
        }

        // STEP 6: Validate activity
        Activity activity = activityRepository.findByCode(activityCode)
                .orElseThrow(() -> new BookingException("Activity not found: " + activityCode));

        if (!activity.isEnabled()) {
            throw new BookingException("Activity is currently unavailable: " + activityCode);
        }

        // STEP 7: Validate date
        if (bookingDate.isBefore(LocalDate.now())) {
            throw new BookingException("Cannot book slots for past dates");
        }

        // STEP 8: Find compatible resources with PESSIMISTIC LOCK
        List<ServiceResource> compatibleResources = resourceRepository
                .findByServiceIdAndActivityCodeWithLock(serviceId, activityCode);

        if (compatibleResources.isEmpty()) {
            throw new BookingException("No resources available for activity: " + activityCode);
        }

        // Filter to only include resources that were in the original pool
        Set<Long> payloadResourceIdSet = new HashSet<>(pooledResourceIds);
        List<ServiceResource> pooledResources = compatibleResources.stream()
                .filter(r -> payloadResourceIdSet.contains(r.getId()))
                .collect(Collectors.toList());

        if (pooledResources.isEmpty()) {
            throw new BookingException("Resources from the selected pool are no longer available");
        }

        // Categorize resources
        List<ServiceResource> exclusiveResources = new ArrayList<>();
        List<ServiceResource> multiActivityResources = new ArrayList<>();

        for (ServiceResource resource : pooledResources) {
            int activityCount = resource.getActivities() != null ? resource.getActivities().size() : 0;
            if (activityCount == 1) {
                exclusiveResources.add(resource);
            } else if (activityCount > 1) {
                multiActivityResources.add(resource);
            }
        }

        // Check availability for the FULL time range
        List<Long> pooledResourceIdsList = pooledResources.stream()
                .map(ServiceResource::getId)
                .toList();

        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookingsForResourcesWithLock(
                pooledResourceIdsList, bookingDate, startTime, endTime);

        List<DisabledSlot> disabledSlots = disabledSlotRepository.findOverlappingDisabledSlots(
                pooledResourceIdsList, bookingDate, startTime, endTime);

        // ═══════════════════════════════════════════════════════════════════════
        // CONTINUE WITH NORMAL BOOKING FLOW
        // ═══════════════════════════════════════════════════════════════════════

        User currentUser = authUtil.getCurrentUser();

        // 1. Try Single Resource Allocation
        ServiceResource availableResource = null;
        String allocationReason = null;

        // PRIORITY 1: Exclusive
        for (ServiceResource resource : exclusiveResources) {
            if (isResourceAvailableForSlots(resource, payloads, overlappingBookings, disabledSlots)) {
                availableResource = resource;
                allocationReason = "EXCLUSIVE (supports only " + activityCode + ")";
                break;
            }
        }

        // PRIORITY 2: Multi-activity
        if (availableResource == null) {
            for (ServiceResource resource : multiActivityResources) {
                if (isResourceAvailableForSlots(resource, payloads, overlappingBookings, disabledSlots)) {
                    availableResource = resource;
                    allocationReason = "MULTI-ACTIVITY";
                    break;
                }
            }
        }

        // 2. Handle Allocation Result
        if (availableResource != null) {
            // SINGLE RESOURCE FOUND - Create merged booking
            log.info("Selected resource {} ({}) for booking - Allocation: {}",
                    availableResource.getName(), availableResource.getId(), allocationReason);

            return createMergedBooking(request, service, availableResource, activityCode, bookingDate, startTime, endTime, payloads, pooledResources);
        } else {
            // SINGLE RESOURCE NOT FOUND - Check for split booking
            if (Boolean.TRUE.equals(request.getAllowSplit())) {
                // Try to allocate each slot independently
                List<BookingResponseDto> childBookings = createSplitBookings(request, service, pooledResources, activityCode, bookingDate, payloads, overlappingBookings, disabledSlots);

                // Return a parent response containing child bookings
                return BookingResponseDto.builder()
                        .bookingType("MULTI_RESOURCE")
                        .status("SUCCESS")
                        .message("Booking created with multiple resources")
                        .childBookings(childBookings)
                        .build();
            } else {
                // Check if split is even possible
                if (canFulfillWithSplit(pooledResources, payloads, overlappingBookings, disabledSlots)) {
                    return BookingResponseDto.builder()
                            .status("PARTIAL_AVAILABLE")
                            .message("Single resource not available for all slots. Please confirm to split booking across resources.")
                            .bookingType("MULTI_RESOURCE")
                            .build();
                } else {
                    throw new BookingException("No available resources for the selected time range. Please try another time.");
                }
            }
        }
    }

    private boolean isResourceAvailableForSlots(ServiceResource resource, List<SlotKeyPayload> payloads,
                                                List<Booking> bookings, List<DisabledSlot> disabledSlots) {
        for (SlotKeyPayload payload : payloads) {
            if (!isResourceAvailableForSlot(resource, payload.getStartTime(), payload.getEndTime(), bookings, disabledSlots)) {
                return false;
            }
        }
        return true;
    }

    private boolean isResourceAvailableForSlot(ServiceResource resource, LocalTime start, LocalTime end,
                                               List<Booking> bookings, List<DisabledSlot> disabledSlots) {
        // Check bookings
        boolean isBooked = bookings.stream()
                .filter(b -> b.getResource().getId().equals(resource.getId()))
                .anyMatch(b -> isTimeRangeOverlap(start, end, b.getStartTime(), b.getEndTime()));

        if (isBooked) return false;

        // Check disabled slots
        boolean isDisabled = disabledSlots.stream()
                .filter(ds -> ds.getResource().getId().equals(resource.getId()))
                .anyMatch(ds -> isTimeRangeOverlap(start, end, ds.getStartTime(), ds.getEndTime()));

        return !isDisabled;
    }

    private boolean canFulfillWithSplit(List<ServiceResource> resources, List<SlotKeyPayload> payloads,
                                        List<Booking> bookings, List<DisabledSlot> disabledSlots) {
        for (SlotKeyPayload payload : payloads) {
            boolean slotCovered = false;
            for (ServiceResource resource : resources) {
                if (isResourceAvailableForSlot(resource, payload.getStartTime(), payload.getEndTime(), bookings, disabledSlots)) {
                    slotCovered = true;
                    break;
                }
            }
            if (!slotCovered) return false;
        }
        return true;
    }

    private BookingResponseDto createMergedBooking(SlotBookingRequestDto request, com.hitendra.turf_booking_backend.entity.Service service,
                                                   ServiceResource resource, String activityCode, LocalDate bookingDate,
                                                   LocalTime startTime, LocalTime endTime, List<SlotKeyPayload> payloads,
                                                   List<ServiceResource> pooledResources) {
        // Calculate Price
        ServiceResource pricingResource = pooledResources.get(0);
        ResourceSlotConfig config = slotConfigRepository.findByResourceId(pricingResource.getId())
                .orElseThrow(() -> new BookingException("Slot configuration not found"));
        List<ResourcePriceRule> priceRules = priceRuleRepository.findEnabledRulesByResourceId(pricingResource.getId());
        boolean isWeekend = isWeekend(bookingDate);

        double totalSlotPrice = 0.0;
        for (SlotKeyPayload payload : payloads) {
             GeneratedSlot tempSlot = GeneratedSlot.builder()
                .startTime(payload.getStartTime())
                .endTime(payload.getEndTime())
                .build();
             totalSlotPrice += calculateDynamicSlotPrice(tempSlot, priceRules, isWeekend, config);
        }

        Double platformFee = Math.round(totalSlotPrice * platformFeeRate) / 100.0;
        Double totalAmount = totalSlotPrice + platformFee;
        totalAmount = Math.round(totalAmount * 100.0) / 100.0;

        // Calculate online and venue amounts based on configurable percentage
        Double onlineAmount = Math.round(totalAmount * onlinePaymentPercent) / 100.0;
        Double venueAmount = Math.round((totalAmount - onlineAmount) * 100.0) / 100.0;

        User user = authUtil.getCurrentUser();
        String reference = generateBookingReference();

        // Determine booking status based on payment method
        // All bookings start as PENDING and await payment via Razorpay
        BookingStatus bookingStatus = BookingStatus.PENDING;
        String paymentMode = request.getPaymentMethod();

        log.info("Creating PENDING booking - awaiting payment via: {}", paymentMode);

        Booking booking = Booking.builder()
                .user(user)
                .service(service)
                .resource(resource)
                .activityCode(activityCode)
                .startTime(startTime)
                .endTime(endTime)
                .bookingDate(bookingDate)
                .amount(totalAmount)
                .onlineAmountPaid(java.math.BigDecimal.valueOf(onlineAmount))
                .venueAmountDue(java.math.BigDecimal.valueOf(venueAmount))
                .reference(reference)
                .status(bookingStatus)
                .paymentMode(paymentMode)
                .createdAt(Instant.now())
                .paymentSource(PaymentSource.BY_USER)
                .idempotencyKey(request.getIdempotencyKey())
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        BookingResponseDto response = convertToResponseDto(savedBooking);
        response.setBookingType("SINGLE_RESOURCE");
        return response;
    }

    private List<BookingResponseDto> createSplitBookings(SlotBookingRequestDto request, com.hitendra.turf_booking_backend.entity.Service service,
                                                         List<ServiceResource> resources, String activityCode, LocalDate bookingDate,
                                                         List<SlotKeyPayload> payloads, List<Booking> bookings, List<DisabledSlot> disabledSlots) {
        List<BookingResponseDto> responses = new ArrayList<>();
        User user = authUtil.getCurrentUser();

        // Pricing setup
        ServiceResource pricingResource = resources.get(0);
        ResourceSlotConfig config = slotConfigRepository.findByResourceId(pricingResource.getId())
                .orElseThrow(() -> new BookingException("Slot configuration not found"));
        List<ResourcePriceRule> priceRules = priceRuleRepository.findEnabledRulesByResourceId(pricingResource.getId());
        boolean isWeekend = isWeekend(bookingDate);

        for (int i = 0; i < payloads.size(); i++) {
            SlotKeyPayload payload = payloads.get(i);
            ServiceResource assignedResource = null;

            // Find resource for this slot
            for (ServiceResource resource : resources) {
                if (isResourceAvailableForSlot(resource, payload.getStartTime(), payload.getEndTime(), bookings, disabledSlots)) {
                    assignedResource = resource;
                    break;
                }
            }

            if (assignedResource == null) {
                throw new BookingException("Resource unavailable for slot: " + payload.getStartTime());
            }

            // Calculate price for this slot
            GeneratedSlot tempSlot = GeneratedSlot.builder()
                .startTime(payload.getStartTime())
                .endTime(payload.getEndTime())
                .build();
            Double slotPrice = calculateDynamicSlotPrice(tempSlot, priceRules, isWeekend, config);

            // Add fee per slot for split booking? Or total?
            // Usually fee is on total. But here we create separate bookings.
            // Let's apply fee proportionally or per booking.
            // If we create separate bookings, each is a transaction.
            // Let's apply fee per booking for simplicity and correctness of each record.
            Double platformFee = Math.round(slotPrice * platformFeeRate) / 100.0;
            Double totalAmount = slotPrice + platformFee;
            totalAmount = Math.round(totalAmount * 100.0) / 100.0;

            // Calculate online and venue amounts based on configurable percentage
            Double onlineAmount = Math.round(totalAmount * onlinePaymentPercent) / 100.0;
            Double venueAmount = Math.round((totalAmount - onlineAmount) * 100.0) / 100.0;

            String idempotencyKey = request.getIdempotencyKey() != null ? request.getIdempotencyKey() + "-" + i : null;
            String reference = generateBookingReference();

            Booking booking = Booking.builder()
                    .user(user)
                    .service(service)
                    .resource(assignedResource)
                    .activityCode(activityCode)
                    .startTime(payload.getStartTime())
                    .endTime(payload.getEndTime())
                    .bookingDate(bookingDate)
                    .amount(totalAmount)
                    .onlineAmountPaid(java.math.BigDecimal.valueOf(onlineAmount))
                    .venueAmountDue(java.math.BigDecimal.valueOf(venueAmount))
                    .reference(reference)
                    .status(BookingStatus.PENDING)
                    .createdAt(Instant.now())
                    .paymentSource(PaymentSource.BY_USER)
                    .idempotencyKey(idempotencyKey)
                    .build();

            Booking savedBooking = bookingRepository.save(booking);
            responses.add(convertToResponseDto(savedBooking));
        }
        return responses;
    }

    // ==================== BOOKING CANCELLATION ====================

    /**
     * Cancel a booking and restore availability.
     *
     * @param reference Booking reference
     * @return Cancelled booking details
     */
    @Transactional
    public BookingResponseDto cancelBooking(String reference) {
        Booking booking = bookingRepository.findByReferenceWithLock(reference)
                .orElseThrow(() -> new BookingException("Booking not found: " + reference));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingException("Cannot cancel a completed booking");
        }

        // Update status to cancelled
        booking.setStatus(BookingStatus.CANCELLED);
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Cancelled booking {} - slot is now available for rebooking", reference);

        // Process refund if applicable
        try {
            refundService.processRefundForCancelledBooking(savedBooking, "Booking cancelled");
        } catch (Exception e) {
            log.error("Failed to process refund for booking {}: {}", reference, e.getMessage());
            // Don't fail the cancellation if refund processing fails
        }

        // Note: Slot is automatically available for rebooking because
        // we filter by status IN ('CONFIRMED', 'PENDING') in availability checks

        return convertToResponseDto(savedBooking);
    }

    // ==================== HELPER METHODS ====================

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * CATEGORIZE RESOURCES BY PRIORITY FOR BOOKING ALLOCATION
     * ═══════════════════════════════════════════════════════════════════════════
     * This method divides resources into two categories based on how many
     * activities they support. This is used for priority-based allocation.
     *
     * @param resources List of resources to categorize
     * @param requestedActivityCode The activity user wants to book
     * @return CategorizedResources containing exclusive and multi-activity lists
     *
     * ═══════════════════════════════════════════════════════════════════════════
     * LOGIC EXPLANATION:
     * ═══════════════════════════════════════════════════════════════════════════
     *
     * We count how many activities each resource supports:
     *
     * Case 1: Resource supports 1 activity → EXCLUSIVE
     *   - This resource is dedicated to one activity only
     *   - Example: "Cricket Only Ground" with activities = [CRICKET]
     *   - PRIORITY: Book first (keeps multi-activity free for others)
     *
     * Case 2: Resource supports 2+ activities → MULTI-ACTIVITY
     *   - This resource can be used for multiple activities
     *   - Example: "Multi-Sport Ground" with activities = [CRICKET, FOOTBALL]
     *   - PRIORITY: Book second (fallback when exclusive unavailable)
     *
     * ═══════════════════════════════════════════════════════════════════════════
     * REAL-WORLD EXAMPLE:
     * ═══════════════════════════════════════════════════════════════════════════
     *
     * Service: "Sports Arena"
     * Resources:
     *   - Ground A: activities = [CRICKET]           → EXCLUSIVE (1 activity)
     *   - Ground B: activities = [CRICKET, FOOTBALL] → MULTI-ACTIVITY (2 activities)
     *   - Ground C: activities = [FOOTBALL]          → EXCLUSIVE (1 activity, but for Football)
     *
     * User Request: activity = CRICKET
     *
     * Compatible Resources: [Ground A, Ground B] (both support Cricket)
     *
     * Categorization:
     *   - exclusiveResources = [Ground A]     (only 1 activity: Cricket)
     *   - multiActivityResources = [Ground B] (2 activities: Cricket + Football)
     *
     * Booking Priority:
     *   1. First try Ground A (exclusive Cricket)
     *   2. If Ground A booked, try Ground B (multi-activity)
     *   3. If both booked, return "No availability"
     *
     * ═══════════════════════════════════════════════════════════════════════════
     */
    private CategorizedResources categorizeResourcesByPriority(
            List<ServiceResource> resources, String requestedActivityCode) {

        List<ServiceResource> exclusiveResources = new ArrayList<>();
        List<ServiceResource> multiActivityResources = new ArrayList<>();

        for (ServiceResource resource : resources) {
            // Count how many activities this resource supports
            int activityCount = resource.getActivities() != null ? resource.getActivities().size() : 0;

            if (activityCount == 1) {
                // EXCLUSIVE: Only supports ONE activity (the requested one)
                // These are perfect matches - dedicated resources for this activity
                exclusiveResources.add(resource);
            } else if (activityCount > 1) {
                // MULTI-ACTIVITY: Supports MULTIPLE activities
                // These are shared resources - use as fallback
                multiActivityResources.add(resource);
            }
            // activityCount == 0: Should never happen (resource wouldn't match the query)
        }

        log.debug("Categorized {} resources for activity '{}': {} exclusive, {} multi-activity",
                resources.size(), requestedActivityCode,
                exclusiveResources.size(), multiActivityResources.size());

        return new CategorizedResources(exclusiveResources, multiActivityResources);
    }

    /**
     * Helper record to hold categorized resources.
     * Using a record for immutability and clarity.
     */
    private record CategorizedResources(
            List<ServiceResource> exclusiveResources,
            List<ServiceResource> multiActivityResources
    ) {
    }

    /**
     * Group resources by their base price for pooling.
     * Resources with same price are pooled together.
     */
    private Map<Double, List<ServiceResource>> groupResourcesByPrice(List<ServiceResource> resources) {
        Map<Double, List<ServiceResource>> result = new HashMap<>();

        for (ServiceResource resource : resources) {
            ResourceSlotConfig config = slotConfigRepository.findByResourceId(resource.getId()).orElse(null);

            if (config == null || config.getBasePrice() == null) {
                log.warn("Resource {} has no slot config or base price - skipping from pool", resource.getId());
                continue;
            }

            Double basePrice = config.getBasePrice();
            result.computeIfAbsent(basePrice, k -> new ArrayList<>()).add(resource);
        }

        return result;
    }

    /**
     * Count how many resources are booked for a specific time range.
     */
    private int countBookedResources(List<Booking> bookings, LocalTime startTime, LocalTime endTime) {
        return (int) bookings.stream()
                .filter(b -> isTimeRangeOverlap(startTime, endTime, b.getStartTime(), b.getEndTime()))
                .map(b -> b.getResource().getId())
                .distinct()
                .count();
    }

    /**
     * Calculate slot price dynamically using GeneratedSlot and price rules.
     *
     * Price calculation:
     * 1. Start with base price from config
     * 2. Apply price rules based on day type (weekday/weekend) and time
     *
     * @param slot The dynamically generated slot
     * @param priceRules List of price rules for this resource
     * @param isWeekend Whether the date is a weekend
     * @param config The resource slot config
     * @return Slot price (excluding platform fee)
     */
    private Double calculateDynamicSlotPrice(GeneratedSlot slot,
                                              List<ResourcePriceRule> priceRules, boolean isWeekend,
                                              ResourceSlotConfig config) {
        DayType dayType = isWeekend ? DayType.WEEKEND : DayType.WEEKDAY;

        // Start with base price from config
        Double price = config.getBasePrice();

        // Find applicable rules for this time slot and day type
        List<ResourcePriceRule> applicableRules = priceRules.stream()
                .filter(ResourcePriceRule::isEnabled)
                .filter(r -> r.getDayType() == DayType.ALL || r.getDayType() == dayType)
                .filter(r -> isTimeInRange(slot.getStartTime(), r.getStartTime(), r.getEndTime()))
                .sorted((a, b) -> b.getPriority().compareTo(a.getPriority()))
                .toList();

        // Apply highest priority base price override (if any rule sets a custom base price)
        if (!applicableRules.isEmpty() && applicableRules.get(0).getBasePrice() != null) {
            price = applicableRules.get(0).getBasePrice();
        }

        // Add extra charges from all applicable rules
        for (ResourcePriceRule rule : applicableRules) {
            if (rule.getExtraCharge() != null && rule.getExtraCharge() > 0) {
                price += rule.getExtraCharge();
            }
        }

        return price;
    }

    private boolean isTimeInRange(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && time.isBefore(end);
    }

    private boolean isTimeRangeOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    private String generateBookingReference() {
        return "BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }


    /**
     * Convert booking to response DTO.
     * NOTE: Resource ID is included but can be hidden if needed.
     */
    private BookingResponseDto convertToResponseDto(Booking booking) {
        // Calculate price breakdown
        double totalAmount = booking.getAmount();

        // Reverse calculate subtotal from total amount (assuming total = subtotal * 1.02)
        double slotSubtotal = totalAmount / (1 + platformFeeRate / 100);

        // Round subtotal to 2 decimal places
        slotSubtotal = Math.round(slotSubtotal * 100.0) / 100.0;

        // Calculate fee as difference to ensure sum matches total
        double platformFee = totalAmount - slotSubtotal;
        platformFee = Math.round(platformFee * 100.0) / 100.0;

        // Calculate online and venue amounts
        double onlineAmount;
        double venueAmount;

        if (booking.getOnlineAmountPaid() != null) {
            onlineAmount = booking.getOnlineAmountPaid().doubleValue();
        } else {
            onlineAmount = Math.round(totalAmount * onlinePaymentPercent) / 100.0;
        }

        if (booking.getVenueAmountDue() != null) {
            venueAmount = booking.getVenueAmountDue().doubleValue();
        } else {
            venueAmount = Math.round((totalAmount - onlineAmount) * 100.0) / 100.0;
        }

        Boolean venueAmountCollected = booking.getVenueAmountCollected() != null ? booking.getVenueAmountCollected() : false;

        BookingResponseDto.AmountBreakdown amountBreakdown = BookingResponseDto.AmountBreakdown.builder()
                .slotSubtotal(slotSubtotal)
                .platformFeePercent(platformFeeRate)
                .platformFee(platformFee)
                .totalAmount(totalAmount)
                .onlinePaymentPercent(onlinePaymentPercent)
                .onlineAmount(onlineAmount)
                .venueAmount(venueAmount)
                .venueAmountCollected(venueAmountCollected)
                .currency("INR")
                .build();

        return BookingResponseDto.builder()
                .id(booking.getId())
                .reference(booking.getReference())
                .serviceId(booking.getService().getId())
                .serviceName(booking.getService().getName())
                .startTime(booking.getStartTime().format(TIME_FORMATTER))
                .endTime(booking.getEndTime().format(TIME_FORMATTER))
                .bookingDate(booking.getBookingDate())
                .createdAt(booking.getCreatedAt())
                .amountBreakdown(amountBreakdown)
                .status(booking.getStatus().name())
                .build();
    }

    // ==================== ADMIN MANUAL BOOKING ====================

    /**
     * Create manual booking for admin (walk-in customers).
     * Similar to createSlotBooking but:
     * - No user required (user_id = null)
     * - Sets created_by_admin_id
     * - Status = CONFIRMED (not PENDING)
     * - No payment webhooks required
     * - Generates idempotency key automatically
     *
     * @param request Admin manual booking request
     * @param adminProfileId Admin profile ID who is creating the booking
     * @return BookingResponseDto with booking details
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingResponseDto createAdminManualBooking(AdminManualBookingRequestDto request, Long adminProfileId) {

        log.info("Processing admin manual booking request with {} slotKeys",
                request.getSlotKeys() != null ? request.getSlotKeys().size() : 0);

        if (request.getSlotKeys() == null || request.getSlotKeys().isEmpty()) {
            throw new BookingException("At least one slot must be selected");
        }

        // Generate idempotency key automatically for admin bookings
        String idempotencyKey = "ADMIN-" + UUID.randomUUID();

        // Decrypt and parse all slot keys
        List<SlotKeyPayload> payloads = new ArrayList<>();
        for (String slotKey : request.getSlotKeys()) {
            try {
                String jsonPayload = cryptoUtil.decrypt(slotKey);
                SlotKeyPayload payload = objectMapper.readValue(jsonPayload, SlotKeyPayload.class);

                if (payload.getExpiresAt() < Instant.now().getEpochSecond()) {
                    throw new BookingException("One or more slot keys have expired. Please refresh availability.");
                }
                payloads.add(payload);
            } catch (Exception e) {
                log.error("Error decrypting slot key", e);
                throw new BookingException("Invalid or expired slot key");
            }
        }

        // Validate all slots are for same service and date
        Long serviceId = payloads.get(0).getServiceId();
        String activityCode = payloads.get(0).getActivityCode();
        LocalDate bookingDate = payloads.get(0).getDate();

        for (SlotKeyPayload payload : payloads) {
            if (!payload.getServiceId().equals(serviceId)) {
                throw new BookingException("All slots must be for the same service");
            }
            if (!payload.getActivityCode().equals(activityCode)) {
                throw new BookingException("All slots must be for the same activity");
            }
            if (!payload.getDate().equals(bookingDate)) {
                throw new BookingException("All slots must be for the same date");
            }
        }

        // Get service
        com.hitendra.turf_booking_backend.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new BookingException("Service not found with ID: " + serviceId));

        // Validate service is active
        if (!service.isAvailability()) {
            throw new BookingException("Service is currently unavailable");
        }

        // Get admin profile
        AdminProfile adminProfile = adminProfileRepository.findById(adminProfileId)
                .orElseThrow(() -> new BookingException("Admin profile not found"));

        // Find pooled resources (same activity, same price)
        List<Long> resourceIdsList = payloads.stream()
                .flatMap(p -> p.getResourceIds().stream())
                .distinct()
                .collect(Collectors.toList());

        List<ServiceResource> pooledResources = resourceRepository.findAllById(resourceIdsList).stream()
                .filter(r -> r.getActivities().stream().anyMatch(a -> a.getCode().equals(activityCode)))
                .collect(Collectors.toList());

        if (pooledResources.isEmpty()) {
            throw new BookingException("No resources found supporting activity: " + activityCode);
        }

        // Sort payloads by time to get start and end
        payloads.sort(Comparator.comparing(SlotKeyPayload::getStartTime));
        LocalTime startTime = payloads.get(0).getStartTime();
        LocalTime endTime = payloads.get(payloads.size() - 1).getEndTime();

        // Lock resources and find overlapping bookings
        List<Long> pooledResourceIdsList = pooledResources.stream()
                .map(ServiceResource::getId)
                .toList();

        // Find overlapping bookings with pessimistic lock
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookingsForResourcesWithLock(
                pooledResourceIdsList, bookingDate, startTime, endTime);

        // Find disabled slots
        List<DisabledSlot> disabledSlots = disabledSlotRepository.findOverlappingDisabledSlots(
                pooledResourceIdsList, bookingDate, startTime, endTime);

        // Categorize resources by activity count
        List<ServiceResource> exclusiveResources = pooledResources.stream()
                .filter(r -> r.getActivities().size() == 1)
                .toList();

        List<ServiceResource> multiActivityResources = pooledResources.stream()
                .filter(r -> r.getActivities().size() > 1)
                .toList();

        // Try to find available resource
        ServiceResource availableResource = null;
        String allocationReason = null;

        // PRIORITY 1: Exclusive
        for (ServiceResource resource : exclusiveResources) {
            if (isResourceAvailableForSlots(resource, payloads, overlappingBookings, disabledSlots)) {
                availableResource = resource;
                allocationReason = "EXCLUSIVE (supports only " + activityCode + ")";
                break;
            }
        }

        // PRIORITY 2: Multi-activity
        if (availableResource == null) {
            for (ServiceResource resource : multiActivityResources) {
                if (isResourceAvailableForSlots(resource, payloads, overlappingBookings, disabledSlots)) {
                    availableResource = resource;
                    allocationReason = "MULTI-ACTIVITY";
                    break;
                }
            }
        }

        if (availableResource == null) {
            throw new BookingException("No available resource found for the requested slots");
        }

        log.info("Selected resource {} ({}) for admin manual booking - Allocation: {}",
                availableResource.getName(), availableResource.getId(), allocationReason);

        // Calculate total amount using quoted prices from payloads
        double totalAmount = 0.0;
        for (SlotKeyPayload payload : payloads) {
            double slotPrice = payload.getQuotedPrice() != null ? payload.getQuotedPrice() : 0.0;
            totalAmount += slotPrice;
        }

        // Round to 2 decimal places
        totalAmount = Math.round(totalAmount * 100.0) / 100.0;

        // Use amounts from request or default to 0
        java.math.BigDecimal onlineAmountPaid = request.getOnlineAmountPaid() != null
                ? request.getOnlineAmountPaid()
                : java.math.BigDecimal.ZERO;

        java.math.BigDecimal venueAmountDue = java.math.BigDecimal.valueOf(totalAmount).subtract(onlineAmountPaid);

        Boolean venueAmountCollected = request.getVenueAmountCollected() != null
                && request.getVenueAmountCollected().compareTo(java.math.BigDecimal.ZERO) > 0;

        String reference = generateBookingReference();

        // Create booking with CONFIRMED status
        Booking booking = Booking.builder()
                .user(null) // No user for admin manual bookings
                .adminProfile(adminProfile) // Set admin who created this
                .service(service)
                .resource(availableResource)
                .activityCode(activityCode)
                .startTime(startTime)
                .endTime(endTime)
                .bookingDate(bookingDate)
                .amount(totalAmount)
                .onlineAmountPaid(onlineAmountPaid)
                .venueAmountDue(venueAmountDue)
                .venueAmountCollected(venueAmountCollected)
                .reference(reference)
                .status(BookingStatus.CONFIRMED) // Immediately confirmed
                .paymentMode("MANUAL")
                .createdAt(Instant.now())
                .paymentSource(PaymentSource.BY_ADMIN) // Mark as admin-created
                .idempotencyKey(idempotencyKey)
                .paymentStatusEnum(PaymentStatus.SUCCESS) // Mark payment as complete
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        log.info("Admin manual booking created: reference={}, adminId={}", reference, adminProfileId);

        return convertToResponseDto(savedBooking);
    }

    /**
     * Cancel a booking by reference (for admin).
     * Same as user cancellation but accessible by admin.
     *
     * @param reference Booking reference
     * @return Cancelled booking details
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingResponseDto cancelBookingByReference(String reference) {
        return cancelBooking(reference);
    }

    /**
     * Create direct manual booking with all details provided directly.
     * No slot key encryption/decryption required.
     *
     * @param request DirectManualBookingRequestDto with all booking details
     * @param adminProfileId Admin profile ID who is creating the booking
     * @return BookingResponseDto with CONFIRMED status
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingResponseDto createDirectManualBooking(DirectManualBookingRequestDto request, Long adminProfileId) {

        log.info("Processing direct manual booking - Service: {}, Resource: {}, Date: {}, Time: {} to {}",
                request.getServiceId(),
                request.getResourceId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime());

        // Validate booking date is not in the past
        if (request.getBookingDate().isBefore(LocalDate.now())) {
            throw new BookingException("Booking date cannot be in the past");
        }

        // Validate start time before end time
        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().equals(request.getEndTime())) {
            throw new BookingException("Start time must be before end time");
        }

        // Get service
        com.hitendra.turf_booking_backend.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new BookingException("Service not found with ID: " + request.getServiceId()));

        if (!service.isAvailability()) {
            throw new BookingException("Service is currently unavailable");
        }

        // Get resource
        ServiceResource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new BookingException("Resource not found with ID: " + request.getResourceId()));

        if (!resource.isEnabled()) {
            throw new BookingException("Resource is currently disabled");
        }

        // Validate resource belongs to service
        if (!resource.getService().getId().equals(request.getServiceId())) {
            throw new BookingException("Resource does not belong to the specified service");
        }

        // Get admin profile - ensure it's a managed entity
        AdminProfile adminProfile = adminProfileRepository.findById(adminProfileId)
                .orElseThrow(() -> new BookingException("Admin profile not found"));

        log.info("Retrieved AdminProfile - ID: {}, User ID: {}",
                adminProfile.getId(),
                adminProfile.getUser() != null ? adminProfile.getUser().getId() : "null");

        // Check for overlapping bookings
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(
                request.getResourceId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime());

        if (!overlappingBookings.isEmpty()) {
            throw new BookingException("Resource is already booked for the requested time slot");
        }

        // Check for disabled slots
        List<DisabledSlot> disabledSlots = disabledSlotRepository.findOverlappingDisabledSlots(
                Collections.singletonList(request.getResourceId()),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime());

        if (!disabledSlots.isEmpty()) {
            throw new BookingException("Some slots are disabled for the requested time period");
        }

        // Generate idempotency key and reference
        String idempotencyKey = "ADMIN-" + UUID.randomUUID();
        String reference = generateBookingReference();

        // Process payment amounts
        java.math.BigDecimal onlineAmountPaid = request.getOnlineAmountPaid() != null
                ? request.getOnlineAmountPaid()
                : java.math.BigDecimal.ZERO;

        java.math.BigDecimal venueAmountCollected = request.getVenueAmountCollected() != null
                ? request.getVenueAmountCollected()
                : java.math.BigDecimal.ZERO;

        // Calculate venue amount due (total - online paid)
        java.math.BigDecimal venueAmountDue = request.getAmount().subtract(onlineAmountPaid);

        // Check if venue amount has been collected
        Boolean venueCollected = venueAmountCollected.compareTo(java.math.BigDecimal.ZERO) > 0;

        // Create booking with CONFIRMED status
        Booking booking = Booking.builder()
                .user(null)  // No user for admin manual bookings
                .adminProfile(adminProfile)  // Set admin who created this
                .service(service)
                .resource(resource)
                .activityCode(request.getActivityCode())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .bookingDate(request.getBookingDate())
                .amount(request.getAmount().doubleValue())
                .onlineAmountPaid(onlineAmountPaid)
                .venueAmountDue(venueAmountDue)
                .venueAmountCollected(venueCollected)
                .status(BookingStatus.CONFIRMED)  // Immediately confirmed
                .paymentMode("MANUAL")
                .createdAt(Instant.now())
                .paymentSource(PaymentSource.BY_ADMIN)  // Mark as admin-created
                .idempotencyKey(idempotencyKey)
                .paymentStatusEnum(PaymentStatus.SUCCESS)  // Mark payment as complete
                .reference(reference)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        log.info("Direct manual booking created: reference={}, adminId={}, adminProfile set={}, resourceId={}, amount={}",
                reference, adminProfileId, savedBooking.getAdminProfile() != null,
                request.getResourceId(), request.getAmount());

        if (savedBooking.getAdminProfile() != null) {
            log.info("AdminProfile details: id={}, userId={}",
                    savedBooking.getAdminProfile().getId(),
                    savedBooking.getAdminProfile().getUser() != null ? savedBooking.getAdminProfile().getUser().getId() : "null");
        } else {
            log.warn("WARNING: AdminProfile was not set on the booking even though it was specified!");
        }

        return convertToResponseDto(savedBooking);
    }
}

