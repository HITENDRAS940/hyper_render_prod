package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.service.SlotAvailabilityDto;
import com.hitendra.turf_booking_backend.dto.service.SlotStatusResponseDto;
import com.hitendra.turf_booking_backend.dto.service.ServiceSlotAvailabilityDto;
import com.hitendra.turf_booking_backend.service.ServiceSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * @deprecated This controller is obsolete. Slots are now dynamically generated.
 * Use /resources/{resourceId}/availability and /api/slots/availability endpoints instead.
 */
@Deprecated
@RestController
@RequestMapping("/service-slots")
@RequiredArgsConstructor
@Tag(name = "Service Slots (Deprecated)", description = "DEPRECATED: Use resource-based endpoints instead")
public class ServiceSlotController {

    private final ServiceSlotService serviceSlotService;

    @GetMapping("/{serviceId}/availability")
    @Operation(summary = "Get slot availability for a service",
               description = "Get simplified availability status (slotId, available boolean, and price) for all slots of a specific service on a given date")
    public ResponseEntity<List<SlotAvailabilityDto>> getSlotAvailability(
            @PathVariable Long serviceId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        List<SlotAvailabilityDto> availability = serviceSlotService.getSimpleSlotAvailability(serviceId, date);
        return ResponseEntity.ok(availability);
    }

    @GetMapping("/{serviceId}/availability/detailed")
    @Operation(summary = "Get detailed slot availability for a service",
               description = "Get detailed availability information for all slots of a specific service on a given date")
    public ResponseEntity<List<ServiceSlotAvailabilityDto>> getDetailedSlotAvailability(
            @PathVariable Long serviceId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        List<ServiceSlotAvailabilityDto> availability = serviceSlotService.getSlotAvailabilityForDate(serviceId, date);
        return ResponseEntity.ok(availability);
    }

    @GetMapping("/{serviceId}/booked-dates")
    @Operation(summary = "Get all booked dates for service slots",
               description = "Get all booked dates for all slots of a specific service")
    public ResponseEntity<List<ServiceSlotAvailabilityDto>> getAllBookedDates(@PathVariable Long serviceId) {
        List<ServiceSlotAvailabilityDto> bookedDates = serviceSlotService.getAllBookedDatesForService(serviceId);
        return ResponseEntity.ok(bookedDates);
    }

    @GetMapping("/{serviceId}/slots/{slotId}/booked-dates")
    @Operation(summary = "Get booked dates for specific slot",
               description = "Get all booked dates for a specific slot of a service")
    public ResponseEntity<List<LocalDate>> getBookedDatesForSlot(
            @PathVariable Long serviceId,
            @PathVariable Long slotId) {
        List<LocalDate> bookedDates = serviceSlotService.getBookedDatesForSlot(serviceId, slotId);
        return ResponseEntity.ok(bookedDates);
    }

    @PostMapping("/{serviceId}/slots/{slotId}/book")
    @Operation(summary = "Book a slot for a date",
               description = "Book a specific slot for a given date")
    public ResponseEntity<String> bookSlotForDate(
            @PathVariable Long serviceId,
            @PathVariable Long slotId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        boolean booked = serviceSlotService.bookSlotForDate(serviceId, slotId, date);
        if (booked) {
            return ResponseEntity.ok("Slot booked successfully for " + date);
        } else {
            return ResponseEntity.badRequest().body("Slot is already booked for " + date + " or slot not found");
        }
    }

    @DeleteMapping("/{serviceId}/slots/{slotId}/cancel")
    @Operation(summary = "Cancel a slot booking",
               description = "Cancel booking for a specific slot on a given date")
    public ResponseEntity<String> cancelSlotBooking(
            @PathVariable Long serviceId,
            @PathVariable Long slotId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        boolean cancelled = serviceSlotService.cancelSlotBooking(serviceId, slotId, date);
        if (cancelled) {
            return ResponseEntity.ok("Slot booking cancelled successfully for " + date);
        } else {
            return ResponseEntity.badRequest().body("No booking found for " + date + " or slot not found");
        }
    }

    @GetMapping("/{serviceId}/slots/{slotId}/available")
    @Operation(summary = "Check if slot is available for a date",
               description = "Check if a specific slot is available for booking on a given date")
    public ResponseEntity<Boolean> isSlotAvailable(
            @PathVariable Long serviceId,
            @PathVariable Long slotId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        boolean available = serviceSlotService.isSlotAvailable(serviceId, slotId, date);
        return ResponseEntity.ok(available);
    }

    @GetMapping("/{serviceId}/slot-status")
    @Operation(summary = "Get slot status for a service on a date",
               description = "Get slot availability status with separate arrays for disabled and booked slot IDs. Disabled includes slots disabled permanently for the service and slots disabled for the specific date.")
    public ResponseEntity<SlotStatusResponseDto> getSlotStatus(
            @PathVariable Long serviceId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        SlotStatusResponseDto status = serviceSlotService.getSlotStatusForDate(serviceId, date);
        return ResponseEntity.ok(status);
    }
}
