package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.booking.BookingResponseDto;
import com.hitendra.turf_booking_backend.dto.booking.DirectManualBookingRequestDto;
import com.hitendra.turf_booking_backend.dto.booking.SlotAvailabilityResponseDto;
import com.hitendra.turf_booking_backend.dto.common.PaginatedResponse;
import com.hitendra.turf_booking_backend.dto.dashboard.AdminDashboardStatsDto;
import com.hitendra.turf_booking_backend.dto.financial.AdminFinancialOverviewDto;
import com.hitendra.turf_booking_backend.dto.financial.AdminLedgerEntryDto;
import com.hitendra.turf_booking_backend.dto.financial.AdminExpenseRequestDto;
import com.hitendra.turf_booking_backend.dto.financial.AdminExpenseResponseDto;
import com.hitendra.turf_booking_backend.dto.financial.FinancialTransactionDto;
import com.hitendra.turf_booking_backend.dto.financial.SettlementDto;
import com.hitendra.turf_booking_backend.entity.AdminLedgerType;
import com.hitendra.turf_booking_backend.dto.revenue.AdminRevenueReportDto;
import com.hitendra.turf_booking_backend.dto.revenue.ServiceRevenueDto;
import com.hitendra.turf_booking_backend.dto.service.*;
import com.hitendra.turf_booking_backend.dto.slot.DeleteDisabledSlotsRequest;
import com.hitendra.turf_booking_backend.dto.slot.DisabledSlotDto;
import com.hitendra.turf_booking_backend.dto.user.AdminProfileDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.hitendra.turf_booking_backend.dto.slot.UnifiedDisableSlotResponseDto;
import com.hitendra.turf_booking_backend.dto.slot.UnifiedDisableSlotRequestDto;
import com.hitendra.turf_booking_backend.dto.revenue.ServiceTodayRevenueDto;

import java.time.LocalDate;
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
    private final AdminPushTokenService adminPushTokenService;
    private final AdminFinancialService adminFinancialService;

    // ==================== Profile Management ====================

    @GetMapping("/profile")
    @Operation(summary = "Get admin profile", description = "Get current admin's profile information")
    public ResponseEntity<AdminProfileDto> getAdminProfile() {
        Long userId = getCurrentUserId();
        AdminProfileDto profile = adminProfileService.getAdminByUserId(userId);
        return ResponseEntity.ok(profile);
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

        log.info("GET /admin/bookings - userId: {}, adminProfileId: {}, date: {}, status: {}, page: {}, size: {}",
                userId, adminProfile.getId(), date, status, page, size);

        PaginatedResponse<BookingResponseDto> bookings = bookingService.getBookingsByAdminIdWithFilters(
                adminProfile.getId(), date, status, page, size);

        log.info("Returning {} bookings for adminProfileId: {}, page content size: {}",
                bookings.getTotalElements(), adminProfile.getId(), bookings.getContent().size());

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

    @PutMapping("/bookings/{bookingId}/complete")
    @Operation(summary = "Complete booking", description = "Mark a confirmed booking as completed after service has been delivered. Requires venue payment collection details.")
    public ResponseEntity<BookingResponseDto> completeBooking(
            @PathVariable Long bookingId,
            @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Venue payment collection details",
                    required = true
            ) com.hitendra.turf_booking_backend.dto.booking.CompleteBookingRequestDto request) {
        BookingResponseDto completedBooking = bookingService.completeBooking(bookingId, request);
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
    public ResponseEntity<UnifiedDisableSlotResponseDto> disableSlots(
            @Valid @RequestBody UnifiedDisableSlotRequestDto request) {

        Long userId = getCurrentUserId();
        AdminProfileDto adminProfile = adminProfileService.getAdminByUserId(userId);

        UnifiedDisableSlotResponseDto response =
                disabledSlotService.disableSlots(request, adminProfile.getId());

        return ResponseEntity.ok(response);
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
    public ResponseEntity<ServiceTodayRevenueDto> getServiceTodayRevenueReport(
            @PathVariable Long serviceId) {
        ServiceTodayRevenueDto report =
                revenueService.getServiceTodayRevenueReport(serviceId);
        return ResponseEntity.ok(report);
    }

    // ==================== Push Notification Token Management ====================

    @PostMapping("/push-token")
    @Operation(summary = "Save Admin Push Token", description = "Register a push notification token for the current admin")
    public ResponseEntity<Void> savePushToken(@RequestBody java.util.Map<String, String> payload) {
        String token = payload.get("token");
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Long adminId = getCurrentUserId();
        adminPushTokenService.saveToken(adminId, token);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/push-token")
    @Operation(summary = "Remove Admin Push Token", description = "Remove a push notification token on logout")
    public ResponseEntity<Void> removePushToken(@RequestBody java.util.Map<String, String> payload) {
        String token = payload.get("token");
        if (token != null && !token.isEmpty()) {
            Long adminId = getCurrentUserId();
            adminPushTokenService.removeTokenForAdmin(adminId, token);
        }
        return ResponseEntity.ok().build();
    }

    // ==================== Financial Overview ====================

    @GetMapping("/ledger-summary")
    @Operation(summary = "Get admin ledger summary",
               description = "Returns the full financial overview for the currently authenticated admin: " +
                       "cash balance, bank balance, pending online amount, and all lifetime totals.")
    public ResponseEntity<AdminFinancialOverviewDto> getLedgerSummary() {
        // Resolve the admin profile ID for the currently authenticated admin
        Long adminProfileId = adminProfileService.getCurrentAdminProfileId();
        AdminFinancialOverviewDto overview = adminFinancialService.getAdminFinancialOverview(adminProfileId);
        return ResponseEntity.ok(overview);
    }

    // ==================== Admin Ledger Entries ====================

    @GetMapping("/ledger/cash")
    @Operation(summary = "Get admin cash ledger history",
            description = """
                Returns a paginated list of all CASH ledger entries for the current admin (newest first).
                
                Each entry is either:
                - **CREDIT**: cash received at venue (booking payment)
                - **DEBIT**: cash spent (expense e.g., salary, maintenance)
                
                The `balanceAfter` field shows the running cash balance after each transaction.
                """)
    public ResponseEntity<PaginatedResponse<AdminLedgerEntryDto>> getCashLedger(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long adminProfileId = adminProfileService.getCurrentAdminProfileId();
        return ResponseEntity.ok(
                adminFinancialService.getLedgerHistory(adminProfileId, AdminLedgerType.CASH, page, size));
    }

    @GetMapping("/ledger/bank")
    @Operation(summary = "Get admin bank ledger history",
            description = """
                Returns a paginated list of all BANK ledger entries for the current admin (newest first).
                
                Each entry is either:
                - **CREDIT**: bank/UPI received at venue, or platform settlement
                - **DEBIT**: bank payment made (expense e.g., online vendor payment)
                
                The `balanceAfter` field shows the running bank balance after each transaction.
                """)
    public ResponseEntity<PaginatedResponse<AdminLedgerEntryDto>> getBankLedger(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long adminProfileId = adminProfileService.getCurrentAdminProfileId();
        return ResponseEntity.ok(
                adminFinancialService.getLedgerHistory(adminProfileId, AdminLedgerType.BANK, page, size));
    }

    @PostMapping("/expenses")
    @Operation(summary = "Record admin expense",
            description = """
                Record a direct admin-level expense that deducts from cash or bank balance.
                
                **Examples:**
                - Salary paid in cash: `{"paymentMode": "CASH", "amount": 2000, "description": "Salary - John Doe", "category": "SALARY"}`
                - Electricity bill paid online: `{"paymentMode": "BANK", "amount": 5000, "description": "Electricity bill - March 2026", "category": "UTILITIES"}`
                
                **Rules:**
                - `paymentMode` must be `CASH` or `BANK`
                - Amount cannot exceed the current balance of the selected sub-ledger
                
                **Effect:**
                - Deducts from admin's cash or bank balance
                - Creates a DEBIT entry in the admin ledger (full audit trail)
                """)
    public ResponseEntity<AdminExpenseResponseDto> recordExpense(
            @Valid @RequestBody AdminExpenseRequestDto request) {
        Long adminProfileId = adminProfileService.getCurrentAdminProfileId();
        AdminExpenseResponseDto response = adminFinancialService.recordAdminExpense(adminProfileId, request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/settlements")
    @Operation(summary = "Get admin settlement history",
            description = "Returns a paginated list of all settlements made by the platform manager to this admin (newest first).")
    public ResponseEntity<PaginatedResponse<SettlementDto>> getSettlementHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long adminProfileId = adminProfileService.getCurrentAdminProfileId();
        return ResponseEntity.ok(
                adminFinancialService.getSettlementHistory(adminProfileId, page, size));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get admin financial transaction history",
            description = "Returns a paginated audit log of all financial events (ADVANCE_ONLINE, VENUE_CASH, VENUE_BANK, SETTLEMENT, ADMIN_EXPENSE) for the current admin.")
    public ResponseEntity<PaginatedResponse<FinancialTransactionDto>> getTransactionHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long adminProfileId = adminProfileService.getCurrentAdminProfileId();
        return ResponseEntity.ok(
                adminFinancialService.getTransactionHistory(adminProfileId, page, size));
    }

    // ==================== Helper Methods ====================

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImplementation userDetails = (UserDetailsImplementation) authentication.getPrincipal();
        return userDetails.getId();
    }
}
