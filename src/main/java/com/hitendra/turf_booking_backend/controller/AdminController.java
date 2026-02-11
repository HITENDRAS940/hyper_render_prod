package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.booking.AdminBookingRequestDTO;
import com.hitendra.turf_booking_backend.dto.booking.AdminManualBookingRequestDto;
import com.hitendra.turf_booking_backend.dto.booking.BookingResponseDto;
import com.hitendra.turf_booking_backend.dto.booking.PendingBookingDto;
import com.hitendra.turf_booking_backend.dto.booking.SlotAvailabilityResponseDto;
import com.hitendra.turf_booking_backend.dto.common.PaginatedResponse;
import com.hitendra.turf_booking_backend.dto.dashboard.AdminDashboardStatsDto;
import com.hitendra.turf_booking_backend.dto.revenue.AdminRevenueReportDto;
import com.hitendra.turf_booking_backend.dto.revenue.ServiceRevenueDto;
import com.hitendra.turf_booking_backend.dto.service.*;
import com.hitendra.turf_booking_backend.dto.slot.BulkDisableSlotRequestDto;
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

    @PatchMapping("/resources/{resourceId}/enable")
    @Operation(summary = "Enable resource", description = "Enable a disabled resource")
    public ResponseEntity<ServiceResourceDto> enableResource(@PathVariable Long resourceId) {
        ServiceResourceDto resource = serviceResourceService.enableResource(resourceId);
        return ResponseEntity.ok(resource);
    }

    @PatchMapping("/resources/{resourceId}/disable")
    @Operation(summary = "Disable resource", description = "Disable a resource without deleting it")
    public ResponseEntity<ServiceResourceDto> disableResource(@PathVariable Long resourceId) {
        ServiceResourceDto resource = serviceResourceService.disableResource(resourceId);
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

    @GetMapping("/bookings")
    @Operation(summary = "Get all my bookings", description = "Get all bookings for services created by this admin. Optionally filter by date and/or status.")
    public ResponseEntity<PaginatedResponse<BookingResponseDto>> getMyBookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) com.hitendra.turf_booking_backend.entity.BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);
        PaginatedResponse<BookingResponseDto> bookings = bookingService.getBookingsByAdminIdWithFilters(
                adminProfile.getId(), date, status, page, size);
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
    @Operation(summary = "Get bookings by service", description = "Get all bookings for a specific service")
    public ResponseEntity<PaginatedResponse<BookingResponseDto>> getBookingsByService(
            @PathVariable Long serviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<BookingResponseDto> bookings = bookingService.getBookingsByService(serviceId, page, size);
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

    // ==================== Slot Disabling Management ====================

    @PostMapping("/slots/disable")
    @Operation(summary = "Disable slot or time range",
            description = """
                Disable a single slot or time range for a resource on a specific date.
                
                **Single Slot:**
                - Provide resourceId, date, and startTime
                - System will automatically disable the single slot starting at that time
                
                **Time Range:**
                - Provide resourceId, date, startTime, and endTime
                - System will disable all slots within that time range
                
                **Validations:**
                - Cannot disable slots with existing confirmed bookings
                - Start time must match a valid slot boundary
                - Time range must contain at least one valid slot
                
                **Use Cases:**
                - Maintenance windows
                - Private events
                - Resource cleaning
                - Holiday closures for specific time periods
                """)
    public ResponseEntity<List<DisabledSlotDto>> disableSlot(
            @Valid @RequestBody DisableSlotRequestDto request) {

        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);

        List<DisabledSlotDto> disabledSlots = disabledSlotService.disableSlot(request, adminProfile.getId());

        return ResponseEntity.ok(disabledSlots);
    }

    @PostMapping("/slots/disable/bulk")
    @Operation(summary = "Bulk disable slots",
            description = """
                Bulk disable slots for multiple resources, dates, or time ranges.
                
                **Disable Entire Day(s):**
                - Provide startDate (and optionally endDate)
                - Omit startTime and endTime to disable all slots
                
                **Disable Specific Time Range Across Multiple Days:**
                - Provide startDate, endDate, startTime, endTime
                - System will disable that time range on each day
                
                **Multiple Resources:**
                - Provide list of resourceIds
                - OR provide serviceId to disable for all resources in that service
                
                **Examples:**
                
                1. Close entire service for 3 days:
                ```json
                {
                  "serviceId": 1,
                  "startDate": "2026-02-15",
                  "endDate": "2026-02-17",
                  "reason": "Annual maintenance"
                }
                ```
                
                2. Disable morning slots (6 AM - 12 PM) for a week:
                ```json
                {
                  "resourceIds": [1, 2],
                  "startDate": "2026-02-15",
                  "endDate": "2026-02-21",
                  "startTime": "06:00",
                  "endTime": "12:00",
                  "reason": "Morning maintenance"
                }
                ```
                
                Returns the total number of slots disabled.
                """)
    public ResponseEntity<Integer> bulkDisableSlots(
            @Valid @RequestBody BulkDisableSlotRequestDto request) {

        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);

        int disabledCount = disabledSlotService.bulkDisableSlots(request, adminProfile.getId());

        return ResponseEntity.ok(disabledCount);
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
            description = "Get all disabled slots for all resources of a service on a specific date")
    public ResponseEntity<List<DisabledSlotDto>> getDisabledSlotsByService(
            @PathVariable Long serviceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

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

    // ==================== Helper Methods ====================

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImplementation userDetails = (UserDetailsImplementation) authentication.getPrincipal();
        return userDetails.getId();
    }
}
