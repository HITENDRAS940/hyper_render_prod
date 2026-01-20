package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.booking.AdminBookingRequestDTO;
import com.hitendra.turf_booking_backend.dto.booking.BookingResponseDto;
import com.hitendra.turf_booking_backend.dto.service.DisableSlotRequest;
import com.hitendra.turf_booking_backend.dto.service.DisabledSlotDto;
import com.hitendra.turf_booking_backend.dto.service.SlotDto;
import com.hitendra.turf_booking_backend.dto.service.ServiceDto;
import com.hitendra.turf_booking_backend.dto.service.ServiceImageUploadResponse;
import com.hitendra.turf_booking_backend.dto.service.ServiceResourceDto;
import com.hitendra.turf_booking_backend.dto.service.CreateServiceResourceRequest;
import com.hitendra.turf_booking_backend.dto.service.UpdateServiceResourceRequest;
import com.hitendra.turf_booking_backend.entity.BookingStatus;
import com.hitendra.turf_booking_backend.service.BookingService;
import com.hitendra.turf_booking_backend.service.ServiceService;
import com.hitendra.turf_booking_backend.service.ServiceResourceService;
import com.hitendra.turf_booking_backend.service.DisabledSlotService;
import com.hitendra.turf_booking_backend.dto.common.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin", description = "Admin APIs for turf management")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ServiceService serviceService;
    private final BookingService bookingService;
    private final ServiceResourceService serviceResourceService;
    private final DisabledSlotService disabledSlotService;

    @PostMapping(value = "/service/{serviceId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload service images", description = "Upload multiple images (maximum 4) for an existing service to Cloudinary")
    public ResponseEntity<ServiceImageUploadResponse> uploadServiceImages(
            @PathVariable Long serviceId,
            @RequestPart("images") List<MultipartFile> images) {
        ServiceImageUploadResponse response = serviceService.uploadServiceImages(serviceId, images);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/service/{id}")
    @Operation(summary = "Get service", description = "Get service details by ID")
    public ResponseEntity<ServiceDto> getService(@PathVariable Long id) {
        ServiceDto service = serviceService.getServiceById(id);
        return ResponseEntity.ok(service);
    }

//    @PutMapping(value = "/service/{id}")
//    @Operation(summary = "Update service details", description = "Update service")
//    public ResponseEntity<ServiceDto> updateService(
//            @PathVariable Long id,
//            @Valid @RequestBody CreateServiceRequest turfDto
//    ) {
//        ServiceDto updated = serviceService.updateService(id, turfDto);
//        return ResponseEntity.ok(updated);
//    }

//    @DeleteMapping("/service/{id}/images")
//    @Operation(summary = "Delete specific images", description = "Delete specific images from a service")
//    public ResponseEntity<String> deleteSpecificImages(
//            @PathVariable Long id,
//            @RequestBody List<String> imageUrls
//    ) {
//        serviceService.deleteSpecificImages(id, imageUrls);
//        return ResponseEntity.ok("Images deleted successfully");
//    }

//    @DeleteMapping("/service/{id}")
//    @Operation(summary = "Delete service", description = "Delete a service and all its images from Cloudinary")
//    public ResponseEntity<String> deleteService(@PathVariable Long id) {
//        serviceService.deleteService(id);
//        return ResponseEntity.ok("Service and all associated images deleted successfully");
//    }

    @GetMapping("/bookings")
    @Operation(summary = "All bookings", description = "Get all bookings across services")
    public ResponseEntity<PaginatedResponse<BookingResponseDto>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<BookingResponseDto> bookings = bookingService.getAllBookings(page, size);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/bookings/service/{serviceId}")
    @Operation(summary = "Bookings by service", description = "Get bookings for a specific service")
    public ResponseEntity<PaginatedResponse<BookingResponseDto>> getBookingsByService(
            @PathVariable Long serviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<BookingResponseDto> bookings = bookingService.getBookingsByService(serviceId, page, size);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/bookings/status/{status}")
    @Operation(summary = "Bookings by status", description = "Get bookings filtered by status (PENDING, CONFIRMED, CANCELLED)")
    public ResponseEntity<PaginatedResponse<BookingResponseDto>> getBookingsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
            PaginatedResponse<BookingResponseDto> bookings = bookingService.getBookingsByStatus(bookingStatus, page, size);
            return ResponseEntity.ok(bookings);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid booking status. Valid values are: PENDING, CONFIRMED, CANCELLED");
        }
    }

    @GetMapping("/bookings/user/{userId}")
    @Operation(summary = "User bookings", description = "Get all bookings for a specific user")
    public ResponseEntity<PaginatedResponse<BookingResponseDto>> getUserBookings(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<BookingResponseDto> bookings = bookingService.getBookingsByUserId(userId, page, size);
        return ResponseEntity.ok(bookings);
    }

//    @GetMapping("/turf/{id}/bookings")
//    @Operation(summary = "Turf bookings", description = "Get bookings for a specific turf")
//    public ResponseEntity<List<BookingResponseDto>> getTurfBookings(@PathVariable Long id) {
//        List<BookingResponseDto> bookings = bookingService.getBookingsByService(id);
//        return ResponseEntity.ok(bookings);
//    }

    // Note: Slot management endpoints removed - slots are now generated dynamically
    // Use /api/slots/availability?serviceId=X&activityCode=Y&date=Z for slot availability

//    @GetMapping("/cloudinary/test")
//    @Operation(summary = "Test Cloudinary connection", description = "Test if Cloudinary is configured properly")
//    public ResponseEntity<Map<String, Object>> testCloudinaryConnection() {
//        Map<String, Object> response = new HashMap<>();
//        try {
//            // This will test if Cloudinary is initialized properly
//            response.put("status", "success");
//            response.put("message", "Cloudinary is configured and ready");
//            response.put("timestamp", java.time.Instant.now());
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            response.put("status", "error");
//            response.put("message", "Cloudinary configuration error: " + e.getMessage());
//            response.put("timestamp", java.time.Instant.now());
//            return ResponseEntity.status(500).body(response);
//        }
//    }

    @PatchMapping("/service/{id}/available")
    @Operation(summary = "Toggle service availability", description = "Toggle the overall availability of a service")
    public ResponseEntity<String> serviceAvailable(
            @PathVariable("id") Long serviceId
    ) {
        serviceService.serviceAvailable(serviceId);
        return ResponseEntity.ok("Service availability toggled successfully");
    }

    @PatchMapping("/service/{id}/notAvailable")
    @Operation(summary = "Toggle service availability", description = "Toggle the overall availability of a service")
    public ResponseEntity<String> serviceNotAvailable(
            @PathVariable("id") Long serviceId
    ) {
        serviceService.serviceNotAvailable(serviceId);
        return ResponseEntity.ok("service availability toggled successfully");
    }

    @GetMapping("/service/{id}/availability")
    @Operation(summary = "Get service availability", description = "Get the availability status of a service")
    public ResponseEntity<Boolean> getServiceAvailability(@PathVariable Long id) {
        boolean availability = serviceService.getServiceAvailability(id);
        return ResponseEntity.ok(availability);
    }

    @GetMapping("/service/{id}/lowest-price")
    @Operation(summary = "Get lowest slot price", description = "Get the lowest price among all enabled slots for a specific service")
    public ResponseEntity<Double> getLowestSlotPrice(@PathVariable Long id) {
        Double lowestPrice = serviceService.getLowestSlotPrice(id);
        return ResponseEntity.ok(lowestPrice);
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get booking details", description = "Get detailed information about a specific booking")
    public ResponseEntity<BookingResponseDto> getBooking(@PathVariable Long bookingId) {
        BookingResponseDto booking = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(booking);
    }

    @PatchMapping("/booking/{bookingId}/cancel")
    @Operation(summary = "Cancel booking", description = "Cancel a user's booking (Admin only)")
    public ResponseEntity<String> cancelBooking(@PathVariable Long bookingId) {
        bookingService.cancelBookingById(bookingId);
        return ResponseEntity.ok("Booking cancelled successfully");
    }

    @PatchMapping("/booking/cancel-by-reference")
    @Operation(summary = "Cancel booking by reference", description = "Cancel a user's booking using booking reference (Admin only)")
    public ResponseEntity<String> cancelBookingByReference(@RequestParam String reference) {
        bookingService.cancelBooking(reference);
        return ResponseEntity.ok("Booking cancelled successfully");
    }

    @GetMapping("/{userId}/services")
    @Operation(summary = "Get services by admin", description = "Get all services created by a specific admin using their user ID")
    public ResponseEntity<PaginatedResponse<ServiceDto>> getServicesByAdmin(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<ServiceDto> services = serviceService.getServicesByUserId(userId, page, size);
        return ResponseEntity.ok(services);
    }

    @PostMapping("/booking")
    @Operation(summary = "Create booking by admin", description = "Create a new booking request for admin")
    public ResponseEntity<BookingResponseDto> adminBooking(
            @RequestBody AdminBookingRequestDTO request
    ) {
        BookingResponseDto response = bookingService.createAdminBooking(request);
        return ResponseEntity.ok(response);
    }

    // Note: Disabled slot management endpoints removed - slots are now generated dynamically
    // Slot disabling will be handled through a different mechanism in the future

    // ============ Service Resource Management Endpoints ============

    @GetMapping("/service/{serviceId}/resources")
    @Operation(summary = "Get all resources", description = "Get all resources (including disabled) for a specific service")
    public ResponseEntity<List<ServiceResourceDto>> getAllServiceResources(@PathVariable Long serviceId) {
        List<ServiceResourceDto> resources = serviceResourceService.getResourcesByServiceId(serviceId);
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/resource/{resourceId}")
    @Operation(summary = "Get resource by ID", description = "Get a specific resource by its ID")
    public ResponseEntity<ServiceResourceDto> getResourceById(@PathVariable Long resourceId) {
        ServiceResourceDto resource = serviceResourceService.getResourceById(resourceId);
        return ResponseEntity.ok(resource);
    }

    @PostMapping("/resource")
    @Operation(summary = "Create resource", description = "Create a new resource (e.g., Turf 1, Court A) for a service")
    public ResponseEntity<ServiceResourceDto> createResource(
            @Valid @RequestBody CreateServiceResourceRequest request
    ) {
        ServiceResourceDto created = serviceResourceService.createResource(request);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/resource/{resourceId}")
    @Operation(summary = "Update resource", description = "Update an existing resource")
    public ResponseEntity<ServiceResourceDto> updateResource(
            @PathVariable Long resourceId,
            @Valid @RequestBody UpdateServiceResourceRequest request
    ) {
        ServiceResourceDto updated = serviceResourceService.updateResource(resourceId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/resource/{resourceId}")
    @Operation(summary = "Delete resource", description = "Delete a resource")
    public ResponseEntity<String> deleteResource(@PathVariable Long resourceId) {
        serviceResourceService.deleteResource(resourceId);
        return ResponseEntity.ok("Resource deleted successfully");
    }

    @PatchMapping("/resource/{resourceId}/enable")
    @Operation(summary = "Enable resource", description = "Enable a disabled resource")
    public ResponseEntity<ServiceResourceDto> enableResource(@PathVariable Long resourceId) {
        ServiceResourceDto resource = serviceResourceService.enableResource(resourceId);
        return ResponseEntity.ok(resource);
    }

    @PatchMapping("/resource/{resourceId}/disable")
    @Operation(summary = "Disable resource", description = "Disable a resource")
    public ResponseEntity<ServiceResourceDto> disableResource(@PathVariable Long resourceId) {
        ServiceResourceDto resource = serviceResourceService.disableResource(resourceId);
        return ResponseEntity.ok(resource);
    }

    // ============ Slot Management Endpoints ============

    @PostMapping("/slots/disable")
    @Operation(summary = "Disable slot", description = "Disable a specific slot for a resource")
    public ResponseEntity<DisabledSlotDto> disableSlot(@Valid @RequestBody DisableSlotRequest request) {
        DisabledSlotDto disabledSlot = disabledSlotService.disableSlot(request);
        return ResponseEntity.ok(disabledSlot);
    }

    @DeleteMapping("/slots/{id}/enable")
    @Operation(summary = "Enable slot", description = "Enable a previously disabled slot")
    public ResponseEntity<String> enableSlot(@PathVariable Long id) {
        disabledSlotService.enableSlot(id);
        return ResponseEntity.ok("Slot enabled successfully");
    }

    @GetMapping("/resource/{resourceId}/disabled-slots")
    @Operation(summary = "Get disabled slots", description = "Get all disabled slots for a resource")
    public ResponseEntity<List<DisabledSlotDto>> getDisabledSlots(@PathVariable Long resourceId) {
        List<DisabledSlotDto> disabledSlots = disabledSlotService.getDisabledSlots(resourceId);
        return ResponseEntity.ok(disabledSlots);
    }
}
