package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.booking.ManualUpiBookingRequestDto;
import com.hitendra.turf_booking_backend.dto.booking.ManualUpiBookingResponseDto;
import com.hitendra.turf_booking_backend.dto.booking.PriceBreakdownDto;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.exception.BookingException;
import com.hitendra.turf_booking_backend.repository.*;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for MANUAL UPI PAYMENT BOOKING with SOFT-LOCKING and AUTO-EXPIRY.
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * CORE RESPONSIBILITIES:
 * ═══════════════════════════════════════════════════════════════════════════════════
 * - Slot availability enforcement
 * - Soft-locking slots during payment (10 minutes)
 * - Preventing double booking
 * - Supporting resume booking for same user
 * - Auto-expiring unpaid bookings
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * BOOKING FLOW:
 * ═══════════════════════════════════════════════════════════════════════════════════
 * 1. User requests to book a slot
 * 2. Backend checks for existing active bookings:
 *    - If SAME user has PAYMENT_PENDING booking → RESUME (return existing)
 *    - If DIFFERENT user has active booking → REJECT (slot unavailable)
 *    - If NO active booking → CREATE new booking with soft-lock
 * 3. Create booking with:
 *    - status = PAYMENT_PENDING
 *    - paymentMode = MANUAL_UPI
 *    - lockExpiresAt = now + 10 minutes
 * 4. Return booking details with payment instructions
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * SOFT-LOCKING RULES:
 * ═══════════════════════════════════════════════════════════════════════════════════
 * - Slot is UNAVAILABLE if: status = PAYMENT_PENDING AND lockExpiresAt > now
 * - Slot becomes AVAILABLE if: lockExpiresAt <= now
 * - Lock duration: 10 minutes (configurable)
 * - Lock is NOT extended on retries
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * RESUME BOOKING:
 * ═══════════════════════════════════════════════════════════════════════════════════
 * - If user retries booking for same slot within lock time:
 *   - Return existing PAYMENT_PENDING booking
 *   - Do NOT create new booking
 *   - Do NOT extend lock time
 *
 * ═══════════════════════════════════════════════════════════════════════════════════
 * EDGE CASES HANDLED:
 * ═══════════════════════════════════════════════════════════════════════════════════
 * - App closed during payment
 * - UPI app cancelled
 * - User retries booking
 * - Multiple users racing for same slot
 * - Late payment after expiry (must NOT confirm)
 * - Concurrent booking requests (pessimistic locking)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManualUpiBookingService {

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceResourceRepository resourceRepository;
    private final ActivityRepository activityRepository;
    private final AuthUtil authUtil;
    private final PricingService pricingService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");
    private static final String PAYMENT_MODE_MANUAL_UPI = "MANUAL_UPI";

    @Value("${booking.lock-duration-minutes:10}")
    private int lockDurationMinutes;

    @Value("${pricing.platform-fee-rate:2.0}")
    private Double platformFeeRate;

    /**
     * Create or resume a manual UPI booking with soft-locking.
     *
     * ═══════════════════════════════════════════════════════════════════════════
     * TRANSACTION ISOLATION: SERIALIZABLE
     * ═══════════════════════════════════════════════════════════════════════════
     * - Prevents race conditions when multiple users book the same slot
     * - Ensures consistent read of booking state
     *
     * @param request Booking request
     * @return Booking response with lock details
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ManualUpiBookingResponseDto createOrResumeBooking(ManualUpiBookingRequestDto request) {
        log.info("Processing manual UPI booking request: serviceId={}, activity={}, date={}, time={}-{}",
                request.getServiceId(), request.getActivityCode(), request.getBookingDate(),
                request.getStartTime(), request.getEndTime());

        // ═══════════════════════════════════════════════════════════════════════
        // STEP 1: VALIDATE INPUTS
        // ═══════════════════════════════════════════════════════════════════════
        validateRequest(request);

        User currentUser = authUtil.getCurrentUser();

        // ═══════════════════════════════════════════════════════════════════════
        // STEP 2: CHECK IDEMPOTENCY (if client request ID provided)
        // ═══════════════════════════════════════════════════════════════════════
        if (request.getClientRequestId() != null && !request.getClientRequestId().isBlank()) {
            Optional<Booking> existingByIdempotency = bookingRepository.findByIdempotencyKey(
                    request.getClientRequestId());
            if (existingByIdempotency.isPresent()) {
                Booking existing = existingByIdempotency.get();
                log.info("Idempotent request detected. Returning existing booking: {}",
                        existing.getReference());
                return convertToResponseDto(existing, true);
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // STEP 3: VALIDATE SERVICE AND ACTIVITY
        // ═══════════════════════════════════════════════════════════════════════
        com.hitendra.turf_booking_backend.entity.Service service = serviceRepository
                .findById(request.getServiceId())
                .orElseThrow(() -> new BookingException("Service not found: " + request.getServiceId()));

        if (!service.isAvailability()) {
            throw new BookingException("Service is currently unavailable");
        }

        Activity activity = activityRepository.findByCode(request.getActivityCode())
                .orElseThrow(() -> new BookingException("Activity not found: " + request.getActivityCode()));

        if (!activity.isEnabled()) {
            throw new BookingException("Activity is currently unavailable: " + request.getActivityCode());
        }

        // ═══════════════════════════════════════════════════════════════════════
        // STEP 4: VALIDATE BOOKING DATE AND TIME
        // ═══════════════════════════════════════════════════════════════════════
        if (request.getBookingDate().isBefore(LocalDate.now())) {
            throw new BookingException("Cannot book slots for past dates");
        }

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BookingException("Start time must be before end time");
        }

        // ═══════════════════════════════════════════════════════════════════════
        // STEP 5: FIND COMPATIBLE RESOURCES
        // ═══════════════════════════════════════════════════════════════════════
        List<ServiceResource> compatibleResources = resourceRepository
                .findByServiceIdAndActivityCode(request.getServiceId(), request.getActivityCode());

        if (compatibleResources.isEmpty()) {
            throw new BookingException("No resources available for activity: " + request.getActivityCode());
        }

        // ═══════════════════════════════════════════════════════════════════════
        // STEP 6: PRIORITY-BASED RESOURCE ALLOCATION
        // ═══════════════════════════════════════════════════════════════════════
        // Try exclusive resources first, then multi-activity resources
        List<ServiceResource> exclusiveResources = new ArrayList<>();
        List<ServiceResource> multiActivityResources = new ArrayList<>();

        for (ServiceResource resource : compatibleResources) {
            int activityCount = resource.getActivities() != null ? resource.getActivities().size() : 0;
            if (activityCount == 1) {
                exclusiveResources.add(resource);
            } else if (activityCount > 1) {
                multiActivityResources.add(resource);
            }
        }

        List<ServiceResource> prioritizedResources = new ArrayList<>();
        prioritizedResources.addAll(exclusiveResources);
        prioritizedResources.addAll(multiActivityResources);

        log.info("Resource allocation: {} exclusive, {} multi-activity",
                exclusiveResources.size(), multiActivityResources.size());

        // ═══════════════════════════════════════════════════════════════════════
        // STEP 7: CHECK FOR EXISTING ACTIVE BOOKINGS AND FIND AVAILABLE RESOURCE
        // ═══════════════════════════════════════════════════════════════════════
        Instant now = Instant.now();
        ServiceResource selectedResource = null;
        Booking existingUserBooking = null;

        for (ServiceResource resource : prioritizedResources) {
            // Find active soft-locked bookings for this resource and time slot
            List<Booking> activeBookings = bookingRepository.findActiveSoftLockedBookings(
                    resource.getId(),
                    request.getBookingDate(),
                    request.getStartTime(),
                    request.getEndTime(),
                    now
            );

            // Also check confirmed bookings
            List<Booking> confirmedBookings = bookingRepository.findOverlappingBookings(
                    resource.getId(),
                    request.getBookingDate(),
                    request.getStartTime(),
                    request.getEndTime()
            );

            boolean hasConfirmedBooking = !confirmedBookings.isEmpty();

            if (!activeBookings.isEmpty()) {
                // Check if any active booking belongs to current user (RESUME case)
                Optional<Booking> userBooking = activeBookings.stream()
                        .filter(b -> b.getUser().getId().equals(currentUser.getId()))
                        .findFirst();

                if (userBooking.isPresent()) {
                    existingUserBooking = userBooking.get();
                    log.info("Found existing PAYMENT_PENDING booking for user: {}",
                            existingUserBooking.getReference());
                    break;  // Resume this booking
                } else {
                    // Different user has locked this slot
                    log.debug("Resource {} is soft-locked by another user", resource.getId());
                    continue;  // Try next resource
                }
            } else if (hasConfirmedBooking) {
                // Slot is confirmed by someone else
                log.debug("Resource {} has confirmed booking", resource.getId());
                continue;  // Try next resource
            } else {
                // Resource is available!
                selectedResource = resource;
                log.info("Selected available resource: {} ({})", resource.getId(), resource.getName());
                break;
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // STEP 8: HANDLE RESUME OR CREATE NEW BOOKING
        // ═══════════════════════════════════════════════════════════════════════

        if (existingUserBooking != null) {
            // RESUME: Return existing booking
            log.info("Resuming existing booking: {}", existingUserBooking.getReference());
            return convertToResponseDto(existingUserBooking, true);
        }

        if (selectedResource == null) {
            // No resources available
            throw new BookingException(
                    "No available resources for the selected time slot. All slots are currently booked or locked.");
        }

        // ═══════════════════════════════════════════════════════════════════════
        // STEP 9: CALCULATE PRICE
        // ═══════════════════════════════════════════════════════════════════════
        PriceBreakdownDto priceBreakdown = pricingService.calculatePriceBreakdownForTimeRange(
                selectedResource.getId(),
                request.getStartTime(),
                request.getEndTime(),
                request.getBookingDate()
        );

        Double totalAmount = priceBreakdown.getTotalAmount();

        // ═══════════════════════════════════════════════════════════════════════
        // STEP 10: CREATE BOOKING WITH SOFT-LOCK
        // ═══════════════════════════════════════════════════════════════════════
        String reference = generateBookingReference();
        Instant lockExpiresAt = now.plus(Duration.ofMinutes(lockDurationMinutes));

        Booking booking = Booking.builder()
                .user(currentUser)
                .service(service)
                .resource(selectedResource)
                .activityCode(request.getActivityCode())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .bookingDate(request.getBookingDate())
                .amount(totalAmount)
                .reference(reference)
                .status(BookingStatus.PAYMENT_PENDING)
                .paymentMode(PAYMENT_MODE_MANUAL_UPI)
                .lockExpiresAt(lockExpiresAt)
                .createdAt(now)
                .paymentSource(PaymentSource.BY_USER)
                .idempotencyKey(request.getClientRequestId())
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        log.info("Created PAYMENT_PENDING booking {} for user {} - lock expires at {}",
                reference, currentUser.getPhone(), lockExpiresAt);

        return convertToResponseDto(savedBooking, false);
    }

    /**
     * Cancel a booking (user-initiated or admin).
     * Sets status to CANCELLED and releases lock immediately.
     */
    @Transactional
    public ManualUpiBookingResponseDto cancelBooking(String reference) {
        Booking booking = bookingRepository.findByReference(reference)
                .orElseThrow(() -> new BookingException("Booking not found: " + reference));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingException("Booking is already cancelled");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingException("Cannot cancel a completed booking");
        }

        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            throw new BookingException("Cannot cancel a confirmed booking via this method. Use refund flow.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setLockExpiresAt(Instant.now());  // Release lock immediately

        Booking savedBooking = bookingRepository.save(booking);

        log.info("Cancelled booking: {}", reference);

        return convertToResponseDto(savedBooking, false);
    }

    /**
     * Confirm payment for a booking (called after admin verifies UPI payment).
     * Transitions status from PAYMENT_PENDING to CONFIRMED.
     */
    @Transactional
    public ManualUpiBookingResponseDto confirmPayment(String reference) {
        Booking booking = bookingRepository.findByReference(reference)
                .orElseThrow(() -> new BookingException("Booking not found: " + reference));

        if (booking.getStatus() != BookingStatus.PAYMENT_PENDING) {
            throw new BookingException("Booking is not in PAYMENT_PENDING status. Current status: "
                    + booking.getStatus());
        }

        // Check if lock has expired
        if (booking.getLockExpiresAt() != null && booking.getLockExpiresAt().isBefore(Instant.now())) {
            throw new BookingException("Payment confirmation failed. Booking lock has expired. " +
                    "Please create a new booking.");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setLockExpiresAt(null);  // Clear lock
        booking.setPaymentTime(Instant.now());

        Booking savedBooking = bookingRepository.save(booking);

        log.info("Confirmed payment for booking: {}", reference);

        return convertToResponseDto(savedBooking, false);
    }

    // ==================== HELPER METHODS ====================

    private void validateRequest(ManualUpiBookingRequestDto request) {
        if (request.getServiceId() == null) {
            throw new BookingException("Service ID is required");
        }
        if (request.getActivityCode() == null || request.getActivityCode().isBlank()) {
            throw new BookingException("Activity code is required");
        }
        if (request.getBookingDate() == null) {
            throw new BookingException("Booking date is required");
        }
        if (request.getStartTime() == null) {
            throw new BookingException("Start time is required");
        }
        if (request.getEndTime() == null) {
            throw new BookingException("End time is required");
        }
    }

    private String generateBookingReference() {
        return "BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private ManualUpiBookingResponseDto convertToResponseDto(Booking booking, boolean isResume) {
        // Calculate amount breakdown
        double totalAmount = booking.getAmount();
        double slotSubtotal = totalAmount / (1 + platformFeeRate / 100);
        double platformFee = totalAmount - slotSubtotal;

        slotSubtotal = Math.round(slotSubtotal * 100.0) / 100.0;
        platformFee = Math.round(platformFee * 100.0) / 100.0;

        ManualUpiBookingResponseDto.AmountBreakdown amountBreakdown =
                ManualUpiBookingResponseDto.AmountBreakdown.builder()
                .slotSubtotal(slotSubtotal)
                .platformFeePercent(platformFeeRate)
                .platformFee(platformFee)
                .totalAmount(totalAmount)
                .currency("INR")
                .build();

        // Calculate lock expiry time remaining
        Long lockExpiresInSeconds = null;
        if (booking.getLockExpiresAt() != null) {
            long secondsRemaining = Duration.between(Instant.now(), booking.getLockExpiresAt()).getSeconds();
            lockExpiresInSeconds = Math.max(0, secondsRemaining);
        }

        return ManualUpiBookingResponseDto.builder()
                .bookingId(booking.getId())
                .reference(booking.getReference())
                .status(booking.getStatus().name())
                .serviceId(booking.getService().getId())
                .serviceName(booking.getService().getName())
                .activityCode(booking.getActivityCode())
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime().format(TIME_FORMATTER))
                .endTime(booking.getEndTime().format(TIME_FORMATTER))
                .slotTime(booking.getStartTime().format(TIME_FORMATTER) + " - " +
                        booking.getEndTime().format(TIME_FORMATTER))
                .amount(booking.getAmount())
                .amountBreakdown(amountBreakdown)
                .paymentMode(booking.getPaymentMode())
                .lockExpiresAt(booking.getLockExpiresAt())
                .lockExpiresInSeconds(lockExpiresInSeconds)
                .isResume(isResume)
                .createdAt(booking.getCreatedAt())
                .build();
    }
}

