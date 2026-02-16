package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.booking.AdminBookingRequestDTO;
import com.hitendra.turf_booking_backend.dto.booking.AdminManualBookingRequestDto;
import com.hitendra.turf_booking_backend.dto.booking.BookingResponseDto;
import com.hitendra.turf_booking_backend.dto.booking.DirectManualBookingRequestDto;
import com.hitendra.turf_booking_backend.dto.booking.PendingBookingDto;
import com.hitendra.turf_booking_backend.dto.booking.SlotAvailabilityResponseDto;
import com.hitendra.turf_booking_backend.dto.common.PaginatedResponse;
import com.hitendra.turf_booking_backend.dto.dashboard.AdminDashboardStatsDto;
import com.hitendra.turf_booking_backend.dto.revenue.AdminRevenueReportDto;
import com.hitendra.turf_booking_backend.dto.revenue.ServiceRevenueDto;
import com.hitendra.turf_booking_backend.dto.service.*;
import com.hitendra.turf_booking_backend.dto.slot.BulkDisableSlotRequestDto;
import com.hitendra.turf_booking_backend.dto.slot.DeleteDisabledSlotsRequest;
import com.hitendra.turf_booking_backend.dto.slot.DisableSlotRequestDto;
import com.hitendra.turf_booking_backend.dto.slot.DisabledSlotDto;
import com.hitendra.turf_booking_backend.dto.user.AdminProfileDto;
import com.hitendra.turf_booking_backend.dto.user.DeleteProfileRequest;
import com.hitendra.turf_booking_backend.entity.Booking;
import com.hitendra.turf_booking_backend.entity.Refund;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import com.hitendra.turf_booking_backend.security.service.UserDetailsImplementation;
import com.hitendra.turf_booking_backend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Admin", description = "Admin APIs for managing services, resources, and bookings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminProfileService adminProfileService;
    private final ServiceService serviceService;
    private final BookingService bookingService;
    private final ServiceResourceService serviceResourceService;
    private final ResourceSlotService resourceSlotService;
    private final PricingService pricingService;
    private final RevenueService revenueService;
    private final DashboardService dashboardService;
    private final SlotBookingService slotBookingService;
    private final DisabledSlotService disabledSlotService;
    private final RefundService refundService;
    private final BookingRepository bookingRepository;

    // ==================== Profile Management ====================

    @GetMapping("/profile")
    @Operation(summary = "Get admin profile", description = "Get current admin's profile information")
    public ResponseEntity<AdminProfileDto> getAdminProfile() {
        Long userId = getCurrentUserId();
        AdminProfileDto profile = adminProfileService.getAdminByUserId(userId);
        return ResponseEntity.ok(profile);
    }

    @DeleteMapping("/profile")
    @Operation(summary = "Delete admin account",
            description = "Permanently delete the admin's account. This action is IRREVERSIBLE. " +
                    "All personal data (name, email, phone, business info) will be permanently removed. " +
                    "Booking records will be preserved in anonymized form for business records. " +
                    "Services will remain active but without admin PII. " +
                    "User must confirm by sending confirmationText as 'DELETE MY ACCOUNT'.")
    public ResponseEntity<String> deleteProfile(@RequestBody DeleteProfileRequest request) {
        // Validate confirmation with explicit confirmation text
        if (request.getConfirmationText() == null || !request.getConfirmationText().equals("DELETE MY ACCOUNT")) {
            throw new RuntimeException("Please confirm deletion by setting confirmationText to 'DELETE MY ACCOUNT'");
        }

        Long userId = getCurrentUserId();
        adminProfileService.permanentlyDeleteAdminAccount(userId);
        return ResponseEntity.ok("Your account has been permanently deleted. " +
                "All personal data has been removed. " +
                "You will be logged out and cannot access this account again.");
    }

    // ==================== Dashboard Statistics ====================

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Get dashboard statistics",
            description = "Get comprehensive dashboard statistics for the current admin including today's and monthly booking counts " +
                    "and revenue broken down by online vs offline bookings. Includes all services managed by the admin.")
    public ResponseEntity<AdminDashboardStatsDto> getDashboardStats() {
        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);
        AdminDashboardStatsDto stats = dashboardService.getAdminDashboardStats(adminProfile.getId());
        return ResponseEntity.ok(stats);
    }

    // ==================== Service Management ====================

    @GetMapping("/services")
    @Operation(summary = "Get my services", description = "Get all services created by the current admin (lightweight summary)")
    public ResponseEntity<PaginatedResponse<AdminServiceSummaryDto>> getMyServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        PaginatedResponse<AdminServiceSummaryDto> services = serviceService.getAdminServiceSummaryByUserId(userId, page, size);
        return ResponseEntity.ok(services);
    }

    @GetMapping("/services/{serviceId}")
    @Operation(summary = "Get service by ID", description = "Get detailed information about a specific service")
    public ResponseEntity<ServiceDto> getServiceById(@PathVariable Long serviceId) {
        ServiceDto service = serviceService.getServiceById(serviceId);
        return ResponseEntity.ok(service);
    }

    @PostMapping("/services")
    @Operation(summary = "Create service",
            description = "Create a new service with basic details, activities, and amenities. " +
                    "Request body should include: name, location, city, latitude, longitude, description, contactNumber, " +
                    "activityCodes (list of activity codes like CRICKET, FOOTBALL), " +
                    "amenities (list of amenity names like Parking, WiFi, Cafeteria, Lighting)")
    public ResponseEntity<ServiceDto> createService(@Valid @RequestBody CreateServiceRequest request) {
        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);
        ServiceDto created = serviceService.createServiceDetails(request, adminProfile.getId());
        return ResponseEntity.ok(created);
    }

    @PutMapping("/services/{serviceId}")
    @Operation(summary = "Update service", description = "Update service details")
    public ResponseEntity<ServiceDto> updateService(
            @PathVariable Long serviceId,
            @Valid @RequestBody CreateServiceRequest request) {
        ServiceDto updatedService = serviceService.updateService(serviceId, request);
        return ResponseEntity.ok(updatedService);
    }

    @DeleteMapping("/services/{serviceId}")
    @Operation(summary = "Delete service", description = "Delete a service and all its images")
    public ResponseEntity<String> deleteService(@PathVariable Long serviceId) {
        serviceService.deleteService(serviceId);
        return ResponseEntity.ok("Service deleted successfully");
    }

    @PatchMapping("/services/{serviceId}/toggle")
    @Operation(summary = "Toggle service availability status",
            description = """
                Toggle a service's available/unavailable status.
                - If available: will be marked unavailable
                - If unavailable: will be marked available
                
                **Use Cases:**
                - Quickly toggle service availability without separate API calls
                - Dynamic service availability management
                - Temporary service closure/reopening
                
                **Response:**
                Returns updated service with new availability status
                
                **Note:**
                - Does not delete the service
                - All service data remains intact
                - Bookings are not affected, but new bookings cannot be made if unavailable
                """)
    public ResponseEntity<ServiceDto> toggleService(@PathVariable Long serviceId) {
        // Check current service availability status
        Boolean isCurrentlyAvailable = serviceService.isServiceAvailable(serviceId);

        // Toggle based on current status
        if (isCurrentlyAvailable) {
            // Service is available, make it unavailable
            serviceService.serviceNotAvailable(serviceId);
        } else {
            // Service is unavailable, make it available
            serviceService.serviceAvailable(serviceId);
        }

        // Fetch and return updated service
        ServiceDto service = serviceService.getServiceById(serviceId);
        return ResponseEntity.ok(service);
    }

    @PutMapping("/services/{serviceId}/location-from-url")
    @Operation(summary = "Update service location from URL", description = "Extract latitude and longitude from a Google Maps URL and save to the service")
    public ResponseEntity<ServiceDto> updateServiceLocationFromUrl(
            @PathVariable Long serviceId,
            @Valid @RequestBody LocationUrlRequest request) {
        ServiceDto updated = serviceService.updateServiceLocationFromUrl(serviceId, request.getLocationUrl());
        return ResponseEntity.ok(updated);
    }

    @PostMapping(value = "/services/{serviceId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload service images", description = "Upload multiple images (maximum 4) for an existing service to Cloudinary")
    public ResponseEntity<ServiceImageUploadResponse> uploadServiceImages(
            @PathVariable Long serviceId,
            @RequestPart("images") List<MultipartFile> images) {
        ServiceImageUploadResponse response = serviceService.uploadServiceImages(serviceId, images);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/services/{serviceId}/images")
    @Operation(summary = "Delete specific images", description = "Delete specific images from a service")
    public ResponseEntity<String> deleteServiceImages(
            @PathVariable Long serviceId,
            @RequestBody List<String> imageUrls) {
        serviceService.deleteSpecificImages(serviceId, imageUrls);
        return ResponseEntity.ok("Images deleted successfully");
    }

    // ==================== Resource Management ====================

    @GetMapping("/services/{serviceId}/resources")
    @Operation(summary = "Get service resources", description = "Get all resources for a specific service")
    public ResponseEntity<List<ServiceResourceDto>> getServiceResources(@PathVariable Long serviceId) {
        List<ServiceResourceDto> resources = serviceResourceService.getResourcesByServiceId(serviceId);
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/resources/{resourceId}")
    @Operation(summary = "Get resource by ID", description = "Get detailed information about a specific resource")
    public ResponseEntity<ServiceResourceDto> getResourceById(@PathVariable Long resourceId) {
        ServiceResourceDto resource = serviceResourceService.getResourceById(resourceId);
        return ResponseEntity.ok(resource);
    }

    @PostMapping("/services/{serviceId}/resources")
    @Operation(summary = "Add resource to service",
            description = "Add a new resource to a service with default slot configuration and activities. " +
                    "Request body should include: name, description (optional), enabled, " +
                    "openingTime, closingTime, slotDurationMinutes, basePrice, " +
                    "activityCodes (list of activities this resource supports, e.g., [\"CRICKET\", \"FOOTBALL\"])")
    public ResponseEntity<ServiceResourceDto> addResource(
            @PathVariable Long serviceId,
            @Valid @RequestBody CreateServiceResourceRequest request) {
        request.setServiceId(serviceId);
        ServiceResourceDto created = serviceResourceService.createResource(request);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/resources/{resourceId}")
    @Operation(summary = "Update resource", description = "Update resource details (name, description, enabled)")
    public ResponseEntity<ServiceResourceDto> updateResource(
            @PathVariable Long resourceId,
            @Valid @RequestBody UpdateServiceResourceRequest request) {
        ServiceResourceDto updated = serviceResourceService.updateResource(resourceId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/resources/{resourceId}")
    @Operation(summary = "Delete resource", description = "Delete a resource from the service")
    public ResponseEntity<String> deleteResource(@PathVariable Long resourceId) {
        serviceResourceService.deleteResource(resourceId);
        return ResponseEntity.ok("Resource deleted successfully");
    }

    @PatchMapping("/resources/{resourceId}/toggle")
    @Operation(summary = "Toggle resource status",
            description = """
                Toggle a resource's enabled/disabled status.
                - If enabled: will be disabled
                - If disabled: will be enabled
                
                **Use Cases:**
                - Quick enable/disable without separate API calls
                - Dynamic resource availability management
                
                **Response:**
                Returns updated resource with new enabled status
                """)
    public ResponseEntity<ServiceResourceDto> toggleResource(@PathVariable Long resourceId) {
        // Fetch current resource to check its status
        ServiceResourceDto currentResource = serviceResourceService.getResourceById(resourceId);

        // Toggle based on current status
        ServiceResourceDto resource;
        if (currentResource.isEnabled()) {
            resource = serviceResourceService.disableResource(resourceId);
        } else {
            resource = serviceResourceService.enableResource(resourceId);
        }

        return ResponseEntity.ok(resource);
    }

    // ==================== Slot Configuration Management ====================

    @GetMapping("/resources/{resourceId}/config")
    @Operation(summary = "Get slot configuration", description = "Get the slot configuration for a resource")
    public ResponseEntity<ResourceSlotConfigDto> getSlotConfig(@PathVariable Long resourceId) {
        ResourceSlotConfigDto config = resourceSlotService.getSlotConfig(resourceId);
        return ResponseEntity.ok(config);
    }

    @PostMapping("/resources/slot-config")
    @Operation(summary = "Create/Update slot configuration",
            description = "Create or update slot configuration for a resource (opening time, closing time, slot duration, base price)")
    public ResponseEntity<ResourceSlotConfigDto> createOrUpdateSlotConfig(
            @Valid @RequestBody ResourceSlotConfigRequest request) {
        ResourceSlotConfigDto config = resourceSlotService.createOrUpdateSlotConfig(request);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/resources/{resourceId}/slots")
    @Operation(summary = "Get resource slots with status", description = "Get all slots for a resource on a specific date with their status (AVAILABLE, BOOKED, DISABLED)")
    public ResponseEntity<List<ResourceSlotDetailDto>> getResourceSlots(
            @PathVariable Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ResourceSlotDetailDto> slots = resourceSlotService.getDetailedSlotsByResourceAndDate(resourceId, date);
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/services/{serviceId}/slots/analysis")
    @Operation(summary = "Get comprehensive slot analysis for a service",
            description = "Get detailed analysis of all slots across all resources for a service on a specific date. " +
                    "Shows each slot's status (AVAILABLE, BOOKED, DISABLED), pricing, and availability statistics. " +
                    "Useful for admin to analyze booking patterns and resource utilization.")
    public ResponseEntity<ServiceSlotsAnalysisDto> getServiceSlotsAnalysis(
            @PathVariable Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        ServiceSlotsAnalysisDto analysis = resourceSlotService.getServiceSlotsAnalysis(serviceId, date);
        return ResponseEntity.ok(analysis);
    }

    // ==================== Price Rules Management ====================

    @GetMapping("/resources/{resourceId}/price-rules")
    @Operation(summary = "Get enabled price rules", description = "Get all enabled pricing rules for a resource")
    public ResponseEntity<List<ResourcePriceRuleDto>> getPriceRules(@PathVariable Long resourceId) {
        List<ResourcePriceRuleDto> rules = pricingService.getPriceRulesForResource(resourceId);
        return ResponseEntity.ok(rules);
    }

    @PostMapping("/resources/price-rules")
    @Operation(summary = "Add price rule",
            description = "Add a dynamic pricing rule for a resource (e.g., night lighting charges, peak hours, weekend pricing)")
    public ResponseEntity<ResourcePriceRuleDto> addPriceRule(
            @Valid @RequestBody ResourcePriceRuleRequest request) {
        ResourcePriceRuleDto rule = pricingService.createPriceRule(request);
        return ResponseEntity.ok(rule);
    }

    @PutMapping("/resources/price-rules/{ruleId}")
    @Operation(summary = "Update price rule",
            description = "Update an existing pricing rule (time range, day type, price, etc.)")
    public ResponseEntity<ResourcePriceRuleDto> updatePriceRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody ResourcePriceRuleRequest request) {
        ResourcePriceRuleDto updatedRule = pricingService.updateRuleById(ruleId, request);
        return ResponseEntity.ok(updatedRule);
    }

    @DeleteMapping("/resources/price-rules/{ruleId}")
    @Operation(summary = "Delete price rule", description = "Delete a pricing rule permanently")
    public ResponseEntity<String> deletePriceRule(@PathVariable Long ruleId) {
        pricingService.deletePriceRule(ruleId);
        return ResponseEntity.ok("Price rule deleted successfully");
    }

    @PatchMapping("/resources/price-rules/{ruleId}/enable")
    @Operation(summary = "Enable price rule", description = "Enable a disabled pricing rule")
    public ResponseEntity<ResourcePriceRuleDto> enablePriceRule(@PathVariable Long ruleId) {
        ResourcePriceRuleDto rule = pricingService.togglePriceRule(ruleId, true);
        return ResponseEntity.ok(rule);
    }

    @PatchMapping("/resources/price-rules/{ruleId}/disable")
    @Operation(summary = "Disable price rule", description = "Disable a pricing rule without deleting it")
    public ResponseEntity<ResourcePriceRuleDto> disablePriceRule(@PathVariable Long ruleId) {
        ResourcePriceRuleDto rule = pricingService.togglePriceRule(ruleId, false);
        return ResponseEntity.ok(rule);
    }

    // ==================== Booking Management ====================

    @GetMapping("/bookings/debug")
    @Operation(summary = "Debug bookings query", description = "Debug endpoint to see raw booking data for troubleshooting")
    public ResponseEntity<String> debugBookings() {
        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);

        // Get bookings with eager loading using a dedicated query
        List<com.hitendra.turf_booking_backend.entity.Booking> allBookings =
                bookingRepository.findTop50ByOrderByCreatedAtDesc();

        StringBuilder debug = new StringBuilder();
        debug.append("Current Admin Profile ID: ").append(adminProfile.getId()).append("\n");
        debug.append("Current User ID: ").append(userId).append("\n\n");
        debug.append("Recent Bookings (Top 50):\n");
        debug.append("=".repeat(80)).append("\n");

        for (com.hitendra.turf_booking_backend.entity.Booking booking : allBookings) {
            debug.append("ID: ").append(booking.getId())
                    .append(" | Ref: ").append(booking.getReference())
                    .append(" | Status: ").append(booking.getStatus())
                    .append(" | PaymentSource: ").append(booking.getPaymentSource())
                    .append("\n");

            // Safely access properties
            Long bookingUserId = booking.getUser() != null ? booking.getUser().getId() : null;
            Long bookingAdminProfileId = booking.getAdminProfile() != null ? booking.getAdminProfile().getId() : null;
            Long serviceId = booking.getService() != null ? booking.getService().getId() : null;
            Long serviceCreatedById = (booking.getService() != null && booking.getService().getCreatedBy() != null)
                    ? booking.getService().getCreatedBy().getId() : null;

            debug.append("  User ID: ").append(bookingUserId != null ? bookingUserId : "NULL")
                    .append(" | Admin Profile ID (created_by_admin_id): ")
                    .append(bookingAdminProfileId != null ? bookingAdminProfileId : "NULL")
                    .append("\n");
            debug.append("  Service ID: ").append(serviceId != null ? serviceId : "NULL")
                    .append(" | Service CreatedBy ID: ")
                    .append(serviceCreatedById != null ? serviceCreatedById : "NULL")
                    .append("\n");
            debug.append("  Date: ").append(booking.getBookingDate())
                    .append(" | Amount: ").append(booking.getAmount())
                    .append("\n");

            // Check if this booking should appear for current admin
            boolean shouldAppear = false;
            String reason = "";
            if (serviceCreatedById != null && serviceCreatedById.equals(adminProfile.getId())) {
                shouldAppear = true;
                reason = "service.createdBy matches";
            }
            if (bookingAdminProfileId != null && bookingAdminProfileId.equals(adminProfile.getId())) {
                shouldAppear = true;
                reason += (reason.isEmpty() ? "" : " AND ") + "adminProfile matches";
            }

            debug.append("  >>> Should appear for current admin: ").append(shouldAppear)
                    .append(shouldAppear ? " (" + reason + ")" : "")
                    .append("\n");
            debug.append("-".repeat(80)).append("\n");
        }

        return ResponseEntity.ok(debug.toString());
    }

    @GetMapping("/bookings/test")
    @Operation(summary = "Test booking query", description = "Test endpoint to directly query bookings")
    public ResponseEntity<String> testBookingQuery() {
        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);

        log.info("TEST: Querying bookings for adminId: {}", adminProfile.getId());

        // Try to get projections directly
        var projections = bookingRepository.findBookingsByAdminIdProjected(
                adminProfile.getId(),
                org.springframework.data.domain.PageRequest.of(0, 100)
        );

        StringBuilder result = new StringBuilder();
        result.append("Admin Profile ID: ").append(adminProfile.getId()).append("\n");
        result.append("Total bookings found: ").append(projections.getTotalElements()).append("\n");
        result.append("Page content size: ").append(projections.getContent().size()).append("\n\n");

        for (var proj : projections.getContent()) {
            result.append("ID: ").append(proj.getId())
                    .append(" | Ref: ").append(proj.getReference())
                    .append(" | Status: ").append(proj.getStatus())
                    .append("\n");
        }

        return ResponseEntity.ok(result.toString());
    }

    @GetMapping("/bookings")
    @Operation(summary = "Get all my bookings", description = "Get all bookings for services created by this admin. Optionally filter by date and/or status.")
    public ResponseEntity<PaginatedResponse<BookingResponseDto>> getMyBookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) com.hitendra.turf_booking_backend.entity.BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);

        log.info("GET /admin/bookings - userId: {}, adminProfileId: {}, date: {}, status: {}, page: {}, size: {}",
                userId, adminProfile.getId(), date, status, page, size);

        PaginatedResponse<BookingResponseDto> bookings = bookingService.getBookingsByAdminIdWithFilters(
                adminProfile.getId(), date, status, page, size);

        log.info("Returning {} bookings for adminProfileId: {}, page content size: {}",
                bookings.getTotalElements(), adminProfile.getId(), bookings.getContent().size());

        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/bookings/pending")
    @Operation(summary = "Get pending bookings", description = "Get all bookings with PENDING status for this admin's services")
    public ResponseEntity<PaginatedResponse<PendingBookingDto>> getPendingBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);
        PaginatedResponse<PendingBookingDto> bookings = bookingService.getPendingBookingsByAdminId(adminProfile.getId(), page, size);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/services/{serviceId}/bookings")
    @Operation(summary = "Get bookings by service", description = "Get all bookings for a specific service, optionally filtered by date")
    public ResponseEntity<PaginatedResponse<BookingResponseDto>> getBookingsByService(
            @PathVariable Long serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<BookingResponseDto> bookings = bookingService.getBookingsByService(serviceId, date, page, size);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/resources/{resourceId}/bookings")
    @Operation(summary = "Get bookings by resource", description = "Get all bookings for a specific resource, optionally filtered by date")
    public ResponseEntity<PaginatedResponse<BookingResponseDto>> getBookingsByResource(
            @PathVariable Long resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<BookingResponseDto> bookings = bookingService.getBookingsByResourceAndDate(resourceId, date, page, size);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/bookings/{bookingId}")
    @Operation(summary = "Get booking details", description = "Get detailed information about a specific booking")
    public ResponseEntity<BookingResponseDto> getBookingById(@PathVariable Long bookingId) {
        BookingResponseDto booking = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/bookings/by-reference/{reference}")
    @Operation(summary = "Get booking by reference",
            description = "Get detailed information about a booking using its reference number (e.g., BK-ABC123XYZ). " +
                    "This is useful for quick lookup without needing to know the booking ID.")
    public ResponseEntity<BookingResponseDto> getBookingByReference(@PathVariable String reference) {
        BookingResponseDto booking = bookingService.getBookingByReference(reference);
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/bookings")
    @Operation(summary = "Create booking", description = "Create a booking on behalf of a walk-in customer (auto-confirmed)")
    public ResponseEntity<BookingResponseDto> createAdminBooking(
            @Valid @RequestBody AdminBookingRequestDTO request) {
        BookingResponseDto booking = bookingService.createAdminBooking(request);
        return ResponseEntity.ok(booking);
    }

    @PutMapping("/bookings/{bookingId}/approve")
    @Operation(summary = "Approve booking", description = "Manually approve a pending booking")
    public ResponseEntity<BookingResponseDto> approveBooking(@PathVariable Long bookingId) {
        BookingResponseDto approvedBooking = bookingService.approveBooking(bookingId);
        return ResponseEntity.ok(approvedBooking);
    }

    @PutMapping("/bookings/{bookingId}/complete")
    @Operation(summary = "Complete booking", description = "Mark a confirmed booking as completed after service has been delivered")
    public ResponseEntity<BookingResponseDto> completeBooking(@PathVariable Long bookingId) {
        BookingResponseDto completedBooking = bookingService.completeBooking(bookingId);
        return ResponseEntity.ok(completedBooking);
    }

    @PutMapping("/bookings/{bookingId}/cancel")
    @Operation(summary = "Cancel booking and process refund",
            description = "Cancel a booking by admin. Automatically initiates refund if applicable. " +
                    "Refund amount is calculated based on cancellation policy. Slots will be released.")
    public ResponseEntity<String> cancelBooking(@PathVariable Long bookingId) {
        // Get the booking first
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        // Cancel the booking
        bookingService.cancelBookingById(bookingId);

        // Process refund if applicable
        try {
            Refund refund = refundService.processRefundForCancelledBooking(booking, "Cancelled by admin");
            if (refund != null) {
                return ResponseEntity.ok("Booking cancelled successfully. Refund initiated: ₹" +
                        refund.getRefundAmount() + " (" + refund.getRefundPercent() + "%)");
            }
        } catch (Exception e) {
            log.error("Failed to process refund for booking {}: {}", bookingId, e.getMessage());
            // Don't fail the cancellation if refund processing fails
        }

        return ResponseEntity.ok("Booking cancelled successfully");
    }

    // ==================== Manual Booking (Walk-in Customers) ====================

    @GetMapping("/slots/availability")
    @Operation(summary = "Get slot availability for manual booking",
            description = "Get aggregated slot availability for a service activity to create manual bookings for walk-in customers. " +
                    "Same as user slot availability API but accessible for admin.")
    public ResponseEntity<SlotAvailabilityResponseDto> getSlotAvailabilityForManualBooking(
            @RequestParam Long serviceId,
            @RequestParam String activityCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        SlotAvailabilityResponseDto availability = slotBookingService
                .getAggregatedSlotAvailability(serviceId, activityCode, date);

        return ResponseEntity.ok(availability);
    }

    @PostMapping("/slots/book")
    @Operation(summary = "Create manual booking for walk-in customer",
            description = """
                Create a manual booking for walk-in customers. This is similar to the user booking API but:
                - No user authentication required (user_id will be null)
                - Sets created_by_admin_id to current admin
                - Booking is immediately CONFIRMED (no payment webhook required)
                - Payment details are recorded as provided by admin
                - Idempotency key is auto-generated
                
                **Intent-Based Booking:**
                - Send `slotKeys` from the availability response
                - Backend assigns an available resource automatically
                - Slots must be contiguous
                
                **Payment Details:**
                - Set onlineAmountPaid if customer paid via UPI/card to admin
                - Set venueAmountCollected if cash was collected
                - Can set both if partial payment was made
                
                **Customer Info:**
                - customerName and customerPhone are optional
                - Useful for tracking walk-in customers
                """)
    public ResponseEntity<BookingResponseDto> createManualBooking(
            @Valid @RequestBody AdminManualBookingRequestDto request) {

        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);

        BookingResponseDto booking = slotBookingService.createAdminManualBooking(request, adminProfile.getId());

        return ResponseEntity.status(201).body(booking);
    }

    @PostMapping("/slots/cancel/{reference}")
    @Operation(summary = "Cancel booking by reference",
            description = "Cancel any booking by its reference number. " +
                    "Can be used to cancel both user bookings and walk-in bookings. " +
                    "The slot will become available for rebooking.")
    public ResponseEntity<BookingResponseDto> cancelBookingByReference(
            @PathVariable String reference) {

        BookingResponseDto booking = slotBookingService.cancelBookingByReference(reference);

        return ResponseEntity.ok(booking);
    }

    // ==================== Direct Manual Booking (No Slot Keys) ====================

    @PostMapping("/manual-bookings")
    @Operation(
            summary = "Create direct manual booking for walk-in customer",
            description = """
                Create a manual booking for walk-in customers by providing booking details directly.
                No slot key generation required - just provide all details.
                
                **Booking Details:**
                - Booking is **immediately CONFIRMED** (no payment webhook)
                - No user_id (walk-in customer without account)
                - created_by_admin_id set to current admin
                - Payment details recorded as-is
                - Reference auto-generated (BK-ABC123XYZ)
                - payment_mode: MANUAL
                - payment_source: BY_ADMIN
                
                **Required Fields:**
                - serviceId: ID of service
                - resourceId: Specific resource to book
                - activityCode: Activity code (CRICKET, FOOTBALL, etc.)
                - bookingDate: Date of booking (yyyy-MM-dd)
                - startTime: Start time (HH:mm:ss)
                - endTime: End time (HH:mm:ss)
                - amount: Total booking amount
                
                **Optional Fields:**
                - onlineAmountPaid: Amount paid online (UPI/Card). Default: 0
                - venueAmountCollected: Amount collected at venue (Cash). Default: 0
                - remarks: Notes/tracking info (customer name, phone, etc.)
                
                **Example Request:**
                ```json
                {
                  "serviceId": 1,
                  "resourceId": 5,
                  "activityCode": "CRICKET",
                  "bookingDate": "2026-02-15",
                  "startTime": "06:00:00",
                  "endTime": "08:00:00",
                  "amount": 1000,
                  "onlineAmountPaid": 500,
                  "venueAmountCollected": 500,
                  "remarks": "John Doe, +919876543210, Birthday party"
                }
                ```
                
                **Response:**
                Returns booking with reference and CONFIRMED status
                """)
    public ResponseEntity<BookingResponseDto> createDirectManualBooking(
            @Valid @RequestBody DirectManualBookingRequestDto request) {

        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);

        log.info("Admin {} creating direct manual booking - Service: {}, Resource: {}, Date: {}, Time: {} to {}",
                adminProfile.getId(),
                request.getServiceId(),
                request.getResourceId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime());

        BookingResponseDto booking = slotBookingService.createDirectManualBooking(request, adminProfile.getId());

        log.info("Direct manual booking created successfully - Reference: {}, Admin: {}",
                booking.getReference(), adminProfile.getId());

        return ResponseEntity.status(201).body(booking);
    }

    @GetMapping("/manual-bookings/{reference}")
    @Operation(
            summary = "Get manual booking details by reference",
            description = """
                Retrieve detailed information about a manual booking using its reference number.
                """)
    public ResponseEntity<BookingResponseDto> getManualBookingByReference(
            @PathVariable String reference) {

        log.info("Fetching manual booking details - Reference: {}", reference);

        BookingResponseDto booking = bookingService.getBookingByReference(reference);

        return ResponseEntity.ok(booking);
    }

    // ==================== Slot Disabling Management ====================

    @PostMapping("/slots/disable")
    @Operation(summary = "Disable slots (Unified API)",
            description = """
                Unified API to disable slots - handles all scenarios in a single request:
                
                **1. Single Slot:**
                ```json
                {
                  "resourceIds": [1],
                  "startDate": "2026-02-15",
                  "startTime": "10:00",
                  "reason": "Maintenance"
                }
                ```
                
                **2. Time Range (Same Day):**
                ```json
                {
                  "resourceIds": [1],
                  "startDate": "2026-02-15",
                  "startTime": "10:00",
                  "endTime": "14:00",
                  "reason": "Private event"
                }
                ```
                
                **3. Entire Day:**
                ```json
                {
                  "resourceIds": [1, 2],
                  "startDate": "2026-02-15",
                  "reason": "Holiday"
                }
                ```
                
                **4. Multiple Days (Date Range - All Slots):**
                ```json
                {
                  "serviceId": 1,
                  "startDate": "2026-02-15",
                  "endDate": "2026-02-20",
                  "reason": "Annual maintenance"
                }
                ```
                
                **5. Time Range Across Multiple Days:**
                ```json
                {
                  "resourceIds": [1],
                  "startDate": "2026-02-15",
                  "endDate": "2026-02-20",
                  "startTime": "06:00",
                  "endTime": "12:00",
                  "reason": "Morning renovation"
                }
                ```
                
                **6. Service-Wide Disable:**
                ```json
                {
                  "serviceId": 1,
                  "startDate": "2026-02-15",
                  "startTime": "18:00",
                  "endTime": "22:00",
                  "reason": "Evening maintenance"
                }
                ```
                
                **Parameters:**
                - `resourceIds`: List of resource IDs (use this OR serviceId)
                - `serviceId`: Service ID to disable all its resources (use this OR resourceIds)
                - `startDate`: Required - start date for disable
                - `endDate`: Optional - if provided, disables from startDate to endDate (inclusive)
                - `startTime`: Optional - if not provided, disables entire day(s)
                - `endTime`: Optional - if not provided with startTime, disables single slot
                - `reason`: Optional - reason for disabling
                
                **Validations:**
                - Cannot disable slots with existing confirmed bookings
                - At least one of resourceIds or serviceId must be provided
                - Start time must match a valid slot boundary
                
                **Response:**
                Returns total count, list of disabled slots, and a summary message.
                """)
    public ResponseEntity<com.hitendra.turf_booking_backend.dto.slot.UnifiedDisableSlotResponseDto> disableSlots(
            @Valid @RequestBody com.hitendra.turf_booking_backend.dto.slot.UnifiedDisableSlotRequestDto request) {

        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);

        com.hitendra.turf_booking_backend.dto.slot.UnifiedDisableSlotResponseDto response =
                disabledSlotService.disableSlots(request, adminProfile.getId());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/slots/disabled/{disabledSlotId}")
    @Operation(summary = "Enable a disabled slot",
            description = "Remove a slot from disabled state, making it available for booking again")
    public ResponseEntity<String> enableSlot(@PathVariable Long disabledSlotId) {
        disabledSlotService.enableSlot(disabledSlotId);
        return ResponseEntity.ok("Slot enabled successfully");
    }

    @DeleteMapping("/slots/disabled/by-time")
    @Operation(summary = "Enable slot by time",
            description = "Enable a specific disabled slot by resource, date, and start time")
    public ResponseEntity<String> enableSlotByTime(
            @RequestParam Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime) {

        disabledSlotService.enableSlotByTime(resourceId, date, startTime);
        return ResponseEntity.ok("Slot enabled successfully");
    }

    @DeleteMapping("/resources/{resourceId}/slots/disabled/date/{date}")
    @Operation(summary = "Enable all disabled slots on a date",
            description = "Remove all disabled slots for a resource on a specific date, making them all available")
    public ResponseEntity<Integer> enableAllSlotsOnDate(
            @PathVariable Long resourceId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        int enabledCount = disabledSlotService.enableAllSlotsOnDate(resourceId, date);
        return ResponseEntity.ok(enabledCount);
    }

    @GetMapping("/resources/{resourceId}/slots/disabled")
    @Operation(summary = "Get disabled slots for resource",
            description = "Get all disabled slots for a resource on a specific date")
    public ResponseEntity<List<DisabledSlotDto>> getDisabledSlots(
            @PathVariable Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<DisabledSlotDto> disabledSlots = disabledSlotService.getDisabledSlots(resourceId, date);
        return ResponseEntity.ok(disabledSlots);
    }

    @GetMapping("/services/{serviceId}/slots/disabled")
    @Operation(summary = "Get disabled slots for service",
            description = "Get all disabled slots for all resources of a service. Optionally filter by specific date.")
    public ResponseEntity<List<DisabledSlotDto>> getDisabledSlotsByService(
            @PathVariable Long serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        List<DisabledSlotDto> disabledSlots = disabledSlotService.getDisabledSlotsByService(serviceId, date);
        return ResponseEntity.ok(disabledSlots);
    }

    @GetMapping("/resources/{resourceId}/slots/disabled/range")
    @Operation(summary = "Get disabled slots in date range",
            description = "Get all disabled slots for a resource within a date range")
    public ResponseEntity<List<DisabledSlotDto>> getDisabledSlotsInRange(
            @PathVariable Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<DisabledSlotDto> disabledSlots = disabledSlotService.getDisabledSlotsInRange(resourceId, startDate, endDate);
        return ResponseEntity.ok(disabledSlots);
    }

    @GetMapping("/slots/disabled/all")
    @Operation(summary = "Get all disabled slots for admin's services",
            description = """
                Get all disabled slots across all services managed by the current admin.
                Optionally filter by date range.
                
                **Without Date Filter:**
                Returns all future disabled slots (from today onwards)
                
                **With Date Filter:**
                Returns disabled slots within the specified date range
                
                **Use Cases:**
                - View all upcoming disabled slots across all services
                - Manage disabled slots from a central dashboard
                - Audit which slots are currently disabled
                
                **Response:**
                Each disabled slot includes:
                - Disabled slot ID (for deletion)
                - Resource information
                - Date and time range
                - Reason for disabling
                - Who disabled it and when
                """)
    public ResponseEntity<List<DisabledSlotDto>> getAllDisabledSlotsForAdmin(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);

        List<DisabledSlotDto> disabledSlots = disabledSlotService.getAllDisabledSlotsForAdmin(
                adminProfile.getId(), startDate, endDate);

        return ResponseEntity.ok(disabledSlots);
    }

    @DeleteMapping("/slots/disabled")
    @Operation(summary = "Delete specific disabled slots by IDs",
            description = """
                Delete one or more disabled slots by their IDs, making them available for booking again.
                
                **Request Body:**
                Can be either:
                - Single ID: `{ "disabledSlotIds": [123] }`
                - Multiple IDs: `{ "disabledSlotIds": [123, 456, 789] }`
                
                **Use Cases:**
                - Remove multiple disabled slots at once
                - Bulk enable slots after maintenance completion
                - Clean up expired disabled slots
                
                **Response:**
                Returns the count of successfully deleted (enabled) slots
                
                **Example:**
                ```json
                {
                  "disabledSlotIds": [1, 2, 3, 4, 5]
                }
                ```
                
                **Validation:**
                - Only the admin who owns the service can delete its disabled slots
                - Non-existent IDs are silently skipped
                - Returns count of actually deleted slots
                """)
    public ResponseEntity<String> deleteDisabledSlots(
            @RequestBody DeleteDisabledSlotsRequest request) {

        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);

        int deletedCount = disabledSlotService.deleteDisabledSlotsByIds(
                request.getDisabledSlotIds(), adminProfile.getId());

        String message = deletedCount == 1
                ? "1 disabled slot enabled successfully"
                : deletedCount + " disabled slots enabled successfully";

        return ResponseEntity.ok(message);
    }

    // ==================== Revenue Reporting ====================

    @GetMapping("/revenue")
    @Operation(summary = "Get my revenue report",
            description = "Get complete revenue report for the current admin with service-wise and resource-wise breakdown. " +
                    "Shows total revenue, booking count, and average revenue per booking. " +
                    "Only counts CONFIRMED and COMPLETED bookings.")
    public ResponseEntity<AdminRevenueReportDto> getMyRevenueReport() {
        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);
        AdminRevenueReportDto report = revenueService.getAdminRevenueReport(adminProfile.getId());
        return ResponseEntity.ok(report);
    }

    @GetMapping("/services/{serviceId}/revenue")
    @Operation(summary = "Get service revenue report",
            description = "Get revenue breakdown for a specific service with resource-wise sub-breakdown. " +
                    "Shows which resource generated how much revenue.")
    public ResponseEntity<ServiceRevenueDto> getServiceRevenueReport(@PathVariable Long serviceId) {
        ServiceRevenueDto report = revenueService.getServiceRevenueReport(serviceId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/services/{serviceId}/revenue/today")
    @Operation(summary = "Get today's revenue for service",
            description = """
                Get today's revenue summary for a specific service including:
                - Total bookings confirmed/completed today
                - Total revenue (sum of booking amounts)
                - Amount due today (online amount paid/received)
                - Amount pending at venue (cash to be collected)
                
                **Metrics:**
                - Total Revenue: Sum of all booking amounts for today (₹)
                - Amount Due: Online payments already received (₹)
                - Amount Pending: Cash due to be collected at venue (₹)
                
                **Useful for:**
                - Daily revenue tracking
                - End-of-day cash reconciliation
                - Payment settlement reports
                """)
    public ResponseEntity<com.hitendra.turf_booking_backend.dto.revenue.ServiceTodayRevenueDto> getServiceTodayRevenueReport(
            @PathVariable Long serviceId) {
        com.hitendra.turf_booking_backend.dto.revenue.ServiceTodayRevenueDto report =
                revenueService.getServiceTodayRevenueReport(serviceId);
        return ResponseEntity.ok(report);
    }

    // ==================== Helper Methods ====================

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImplementation userDetails = (UserDetailsImplementation) authentication.getPrincipal();
        return userDetails.getId();
    }
}
