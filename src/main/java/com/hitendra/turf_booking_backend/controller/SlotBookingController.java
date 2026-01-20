package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.booking.*;
import com.hitendra.turf_booking_backend.service.SlotBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller for slot-based booking with resource pooling.
 *
 * DESIGN PRINCIPLES:
 * 1. Resources with same activity AND same price are pooled together
 * 2. Frontend NEVER sees resource IDs when resources are identical
 * 3. Frontend receives aggregated slot availability with availableCount
 * 4. Frontend sends only intent (serviceId, activity, date, slotId)
 * 5. Backend is solely responsible for resource allocation
 *
 * API FLOW:
 * 1. GET /availability - Get aggregated slot availability (public)
 * 2. POST /book - Book a slot using intent (authenticated)
 * 3. POST /cancel - Cancel a booking (authenticated)
 */
@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Slot Booking", description = "APIs for slot-based booking with resource pooling")
public class SlotBookingController {

    private final SlotBookingService slotBookingService;

    // ==================== SLOT AVAILABILITY (Public) ====================

    /**
     * Get aggregated slot availability for a service activity.
     *
     * RESPONSE:
     * - Aggregates availability across all identical resources
     * - Returns availableCount per slot (not individual resource IDs)
     * - Includes pricing and peak hour information
     *
     * POOLING RULES:
     * - Resources are pooled if they support same activity AND have same price
     * - If prices differ, they are NOT pooled (breaks pooling)
     *
     * @param serviceId Service ID
     * @param activityCode Activity code (e.g., "FOOTBALL", "CRICKET")
     * @param date Date to check availability
     * @return SlotAvailabilityResponseDto with aggregated slots
     */
    @GetMapping("/availability")
    @Operation(
        summary = "Get aggregated slot availability",
        description = """
            Returns aggregated slot availability across all identical resources for a service activity.
            
            **Pooling Rules:**
            - Resources are pooled if they support the same activity AND have the same base price
            - If prices differ, resources are NOT pooled
            
            **Response:**
            - `availableCount` shows how many resources are available for each slot
            - `totalCount` shows total pool size
            - No individual resource IDs are exposed
            
            **Usage:**
            Use this endpoint to display available slots to the user.
            For booking, use the `slotId` from the response with POST /book endpoint.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Slot availability retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Service or activity not found"),
        @ApiResponse(responseCode = "409", description = "Service or activity unavailable")
    })
    public ResponseEntity<SlotAvailabilityResponseDto> getSlotAvailability(
            @Parameter(description = "Service ID", required = true)
            @RequestParam Long serviceId,

            @Parameter(description = "Activity code (e.g., FOOTBALL, CRICKET)", required = true)
            @RequestParam String activityCode,

            @Parameter(description = "Date for availability check (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        log.info("Getting slot availability: serviceId={}, activity={}, date={}",
                serviceId, activityCode, date);

        SlotAvailabilityResponseDto availability = slotBookingService
                .getAggregatedSlotAvailability(serviceId, activityCode, date);

        return ResponseEntity.ok(availability);
    }



    /**
     * Book multiple slots as a SINGLE booking.
     *
     * BOOKING FLOW:
     * 1. Validate all inputs (never trust frontend)
     * 2. Check idempotency key (return existing booking if duplicate)
     * 3. Find compatible resources with pessimistic lock
     * 4. Find ONE resource available for the FULL duration
     * 5. Create ONE booking entity
     * 6. Return response
     *
     * IDEMPOTENCY:
     * - If idempotencyKey is provided, duplicate requests return existing booking
     * - Recommended for production use to handle retries
     *
     * @param request Booking request with intent
     * @return BookingResponseDto with merged booking details
     */
    @PostMapping("/book")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    @Operation(
        summary = "Book slots",
        description = """
            Books multiple slots for the specified service activity on the given date.
            
            **Intent-Based Booking:**
            - Frontend sends `slotKeys` (list of encrypted payloads from availability response)
            - Backend automatically assigns an available resource from the pool for the ENTIRE duration
            - Resource allocation is transparent to the user
            
            **Merged Booking:**
            - All requested slots are merged into a single booking entity
            - Slots must be contiguous
            - Total price is calculated for the full duration
            
            **Idempotency:**
            - If `idempotencyKey` is provided, duplicate requests return existing booking
            - Recommended for handling frontend retries
            
            **Availability Check:**
            - Backend validates availability with pessimistic locking
            - Concurrent requests are handled safely
            - If any slot becomes unavailable, 409 Conflict is returned
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Booking created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Service, activity, or slot not found"),
        @ApiResponse(responseCode = "409", description = "Slot unavailable or booking conflict")
    })
    public ResponseEntity<BookingResponseDto> bookSlot(
            @Valid @RequestBody SlotBookingRequestDto request) {

        log.info("Booking slot request received. IdempotencyKey: {}", request.getIdempotencyKey());

        BookingResponseDto booking = slotBookingService.createSlotBooking(request);

        log.info("Booking created: reference={}", booking.getReference());

        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    // ==================== BOOKING CANCELLATION (Authenticated) ====================

    /**
     * Cancel a booking.
     *
     * CANCELLATION FLOW:
     * 1. Find booking by reference with lock
     * 2. Validate booking status
     * 3. Update status to CANCELLED
     * 4. Slot becomes available for rebooking
     *
     * @param reference Booking reference
     * @return Cancelled booking details
     */
    @PostMapping("/cancel/{reference}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    @Operation(
        summary = "Cancel a booking",
        description = """
            Cancels a booking by reference.
            
            **Cancellation Rules:**
            - Only PENDING or CONFIRMED bookings can be cancelled
            - Cancelled bookings cannot be cancelled again
            - Completed bookings cannot be cancelled
            
            **Availability Restoration:**
            - After cancellation, the slot becomes available for rebooking
            - This happens automatically (no manual slot release needed)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Booking not found"),
        @ApiResponse(responseCode = "409", description = "Booking already cancelled or completed")
    })
    public ResponseEntity<BookingResponseDto> cancelBooking(
            @Parameter(description = "Booking reference", required = true)
            @PathVariable String reference) {

        log.info("Cancelling booking: reference={}", reference);

        BookingResponseDto booking = slotBookingService.cancelBooking(reference);

        log.info("Booking cancelled: reference={}", reference);

        return ResponseEntity.ok(booking);
    }
}
