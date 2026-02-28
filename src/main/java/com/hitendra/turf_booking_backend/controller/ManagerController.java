package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.activity.CreateActivityDto;
import com.hitendra.turf_booking_backend.dto.activity.GetActivityDto;
import com.hitendra.turf_booking_backend.dto.appconfig.AppConfigRequest;
import com.hitendra.turf_booking_backend.dto.appconfig.AppConfigResponse;
import com.hitendra.turf_booking_backend.service.AppConfigService;
import com.hitendra.turf_booking_backend.dto.booking.BookingResponseDto;
import com.hitendra.turf_booking_backend.dto.booking.PendingBookingDto;
import com.hitendra.turf_booking_backend.dto.common.PaginatedResponse;
import com.hitendra.turf_booking_backend.dto.financial.AdminDueSummaryDto;
import com.hitendra.turf_booking_backend.dto.financial.AdminFinancialOverviewDto;
import com.hitendra.turf_booking_backend.dto.financial.AdminLedgerEntryDto;
import com.hitendra.turf_booking_backend.dto.financial.FinancialTransactionDto;
import com.hitendra.turf_booking_backend.dto.financial.ManagerGlobalFinancialSummaryDto;
import com.hitendra.turf_booking_backend.dto.financial.ManagerSettlementLedgerEntryDto;
import com.hitendra.turf_booking_backend.dto.financial.SettleRequest;
import com.hitendra.turf_booking_backend.dto.financial.SettlementDto;
import com.hitendra.turf_booking_backend.entity.AdminLedgerType;
import com.hitendra.turf_booking_backend.dto.revenue.AdminRevenueReportDto;
import com.hitendra.turf_booking_backend.dto.revenue.ServiceRevenueDto;
import com.hitendra.turf_booking_backend.dto.service.*;
import com.hitendra.turf_booking_backend.dto.user.AdminProfileDto;
import com.hitendra.turf_booking_backend.dto.user.CreateAdminRequest;
import com.hitendra.turf_booking_backend.dto.user.UserInfoDto;
import com.hitendra.turf_booking_backend.entity.BookingStatus;
import com.hitendra.turf_booking_backend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/manager")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Manager", description = "Manager APIs for admin management")
@PreAuthorize("hasRole('MANAGER')")
public class ManagerController {

    private final AdminProfileService adminProfileService;
    private final ServiceService serviceService;
    private final BookingService bookingService;
    private final ServiceResourceService serviceResourceService;
    private final ResourceSlotService resourceSlotService;
    private final UserService userService;
    private final PricingService pricingService;
    private final RevenueService revenueService;
    private final ActivityService activityService;
    private final AdminFinancialService adminFinancialService;
    private final AppConfigService appConfigService;

    @PostMapping("/admins")
    @Operation(summary = "Create admin", description = "Create a new admin user")
    public ResponseEntity<AdminProfileDto> createAdmin(@Valid @RequestBody CreateAdminRequest request) {
        AdminProfileDto admin = adminProfileService.createAdmin(request);
        return ResponseEntity.ok(admin);
    }

    @GetMapping("/admins")
    @Operation(summary = "List all admins", description = "Get all admin users")
    public ResponseEntity<PaginatedResponse<AdminProfileDto>> getAllAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<AdminProfileDto> admins = adminProfileService.getAllAdmins(page, size);
        return ResponseEntity.ok(admins);
    }

    @GetMapping("/admins/{id}")
    @Operation(summary = "Get admin by ID", description = "Get admin details by ID")
    public ResponseEntity<AdminProfileDto> getAdminById(@PathVariable Long id) {
        AdminProfileDto admin = adminProfileService.getAdminById(id);
        return ResponseEntity.ok(admin);
    }

    @DeleteMapping("/admins/{userId}")
    @Operation(summary = "Delete admin", description = "Delete an admin user")
    public ResponseEntity<String> deleteAdmin(@PathVariable Long userId) {
        adminProfileService.deleteAdmin(userId);
        return ResponseEntity.ok("Admin deleted successfully");
    }

    @DeleteMapping("/services/{serviceId}")
    @Operation(summary = "Force delete a service",
               description = """
                   Permanently delete a service and ALL its associated data:
                   - All resources (turfs, courts, lanes, etc.)
                   - All slot configurations and price rules
                   - All service images (deleted from Cloudinary)
                   
                   **Bookings are preserved** but their `service_id` is set to NULL (audit trail intact).
                   
                   ⚠️ This is irreversible. Use only when you are sure the service must be removed entirely.
                   """)
    public ResponseEntity<String> forceDeleteService(@PathVariable Long serviceId) {
        serviceService.forceDeleteService(serviceId);
        return ResponseEntity.ok("Service " + serviceId + " and all its resources have been permanently deleted.");
    }

    @GetMapping("/services")
    @Operation(summary = "Get all the services", description = "Manager can access all the services on the app")
    public ResponseEntity<PaginatedResponse<ServiceCardDto>> getAllServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<ServiceCardDto> services = serviceService.getAllServicesCard(page, size);
        return ResponseEntity.ok(services);
    }

    @GetMapping("/services/{serviceId}/detail")
    @Operation(
        summary = "Get complete service detail",
        description = "Returns every field of the service including all resources, " +
                      "each resource's slot configuration, and ALL price rules (enabled + disabled). " +
                      "Manager-only endpoint.")
    public ResponseEntity<ServiceDetailDto> getServiceDetail(@PathVariable Long serviceId) {
        return ResponseEntity.ok(serviceService.getServiceDetail(serviceId));
    }

    @GetMapping("/resources/{resourceId}/detail")
    @Operation(
        summary = "Get complete resource detail",
        description = "Returns every field of a resource including its slot configuration " +
                      "and ALL price rules (enabled + disabled). Manager-only endpoint.")
    public ResponseEntity<ResourceDetailDto> getResourceDetail(@PathVariable Long resourceId) {
        return ResponseEntity.ok(serviceResourceService.getResourceDetail(resourceId));
    }

    @GetMapping("/admins/{adminProfileId}/services")
    @Operation(summary = "Get services by admin", description = "Get all services created by a specific admin")
    public ResponseEntity<PaginatedResponse<ServiceDto>> getServicesByAdmin(
            @PathVariable Long adminProfileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<ServiceDto> services = serviceService.getServicesByAdminId(adminProfileId, page, size);
        return ResponseEntity.ok(services);
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

    @PostMapping("/service-details/{adminId}")
    @Operation(summary = "Create service details",
               description = "Create a new service with basic details, activities, and amenities. " +
                       "Request body should include: name, location, city, latitude, longitude, description, contactNumber, " +
                       "activityCodes (list of activity codes like CRICKET, FOOTBALL), " +
                       "amenities (list of amenity names like Parking, WiFi, Cafeteria, Lighting)")
    public ResponseEntity<ServiceDto> createServiceDetails(
            @PathVariable Long adminId,
            @Valid @RequestBody CreateServiceRequest request
    ) {
        ServiceDto created = serviceService.createServiceDetails(request, adminId);
        return ResponseEntity.ok(created);
    }

    // ==================== Resource Management ====================

    @PostMapping("/services/{serviceId}/resources")
    @Operation(summary = "Add resource with slot configuration",
               description = "Add a new resource to a service with default slot configuration and activities. " +
                       "Request body should include: name, description (optional), enabled, " +
                       "openingTime, closingTime, slotDurationMinutes, basePrice, " +
                       "activityCodes (list of activities this resource supports, e.g., [\"CRICKET\", \"FOOTBALL\"])")
    public ResponseEntity<ServiceResourceDto> addResourceWithConfiguration(
            @PathVariable Long serviceId,
            @Valid @RequestBody CreateServiceResourceRequest request
    ) {
        // Ensure serviceId in path matches the request
        request.setServiceId(serviceId);
        ServiceResourceDto created = serviceResourceService.createResource(request);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/services/{serviceId}/location-from-url")
    @Operation(summary = "Update service location from URL", description = "Extract latitude and longitude from a Google Maps URL and save to the service")
    public ResponseEntity<ServiceDto> updateServiceLocationFromUrl(
            @PathVariable Long serviceId,
            @Valid @RequestBody LocationUrlRequest request
    ) {
        ServiceDto updated = serviceService.updateServiceLocationFromUrl(serviceId, request.getLocationUrl());
        return ResponseEntity.ok(updated);
    }


    @GetMapping("/services/{id}/resources")
    @Operation(summary = "Get service resources", description = "Get all resources for a specific service")
    public ResponseEntity<List<ServiceResourceDto>> getServiceResources(@PathVariable Long id) {
        List<ServiceResourceDto> resources = serviceResourceService.getResourcesByServiceId(id);
        return ResponseEntity.ok(resources);
    }

    @PutMapping("/services/{id}")
    @Operation(summary = "Update service", description = "Update service details")
    public ResponseEntity<ServiceDto> updateService(
            @PathVariable Long id,
            @Valid @RequestBody CreateServiceRequest request) {
        ServiceDto updatedService = serviceService.updateService(id, request);
        return ResponseEntity.ok(updatedService);
    }

    @PostMapping(value = "/services/{serviceId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload service images", description = "Upload multiple images (maximum 4) for an existing service to Cloudinary")
    public ResponseEntity<ServiceImageUploadResponse> uploadServiceImages(
            @PathVariable Long serviceId,
            @RequestPart("images") List<MultipartFile> images) {
        ServiceImageUploadResponse response = serviceService.uploadServiceImages(serviceId, images);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/services/{id}/images")
    @Operation(summary = "Delete specific images", description = "Delete specific images from a service")
    public ResponseEntity<String> deleteSpecificImages(
            @PathVariable Long id,
            @RequestBody List<String> imageUrls
    ) {
        serviceService.deleteSpecificImages(id, imageUrls);
        return ResponseEntity.ok("Images deleted successfully");
    }

    @PutMapping("/bookings/{bookingId}/approve")
    @Operation(summary = "Approve booking", description = "Manually approve a booking")
    public ResponseEntity<BookingResponseDto> approveBooking(@PathVariable Long bookingId) {
        BookingResponseDto approvedBooking = bookingService.approveBooking(bookingId);
        return ResponseEntity.ok(approvedBooking);
    }

    @PutMapping("/bookings/{bookingId}/complete")
    @Operation(summary = "Complete booking", description = "Mark a confirmed booking as completed after service has been delivered. Requires service payment collection details.")
    public ResponseEntity<BookingResponseDto> completeBooking(
            @PathVariable Long bookingId,
            @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Venue payment collection details",
                    required = true
            ) com.hitendra.turf_booking_backend.dto.booking.CompleteBookingRequestDto request) {
        BookingResponseDto completedBooking = bookingService.completeBooking(bookingId, request);
        return ResponseEntity.ok(completedBooking);
    }

    @GetMapping("/bookings/pending")
    @Operation(summary = "Get pending bookings", description = "Get all bookings with PENDING status")
    public ResponseEntity<PaginatedResponse<PendingBookingDto>> getPendingBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<PendingBookingDto> bookings = bookingService.getPendingBookingsWithDetails(page, size);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/bookings")
    @Operation(summary = "Get all bookings", description = "Get all bookings across all services")
    public ResponseEntity<PaginatedResponse<BookingResponseDto>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<BookingResponseDto> bookings = bookingService.getAllBookings(page, size);
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

    @PutMapping("/bookings/{bookingId}/cancel")
    @Operation(summary = "Cancel booking", description = "Cancel a booking. Slots will be released.")
    public ResponseEntity<String> cancelBooking(@PathVariable Long bookingId) {
        bookingService.cancelBookingById(bookingId);
        return ResponseEntity.ok("Booking cancelled successfully");
    }

    @GetMapping("/resources/{resourceId}/slots")
    @Operation(summary = "Get resource slots with status", description = "Get all slots for a resource on a specific date with their status (AVAILABLE, BOOKED, DISABLED)")
    public ResponseEntity<List<ResourceSlotDetailDto>> getResourceSlots(
            @PathVariable Long resourceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ResourceSlotDetailDto> slots = resourceSlotService.getDetailedSlotsByResourceAndDate(resourceId, date);
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Get all regular users (excluding admins and managers) with complete information")
    public ResponseEntity<PaginatedResponse<UserInfoDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<UserInfoDto> users = userService.getAllUsers(page, size);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user by ID", description = "Get a specific user's complete information")
    public ResponseEntity<UserInfoDto> getUserById(@PathVariable Long userId) {
        UserInfoDto user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/users/{userId}/bookings")
    @Operation(summary = "Get user bookings", description = "Get all bookings for a specific user")
    public ResponseEntity<PaginatedResponse<BookingResponseDto>> getUserBookings(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<BookingResponseDto> bookings = bookingService.getBookingsByUserId(userId, page, size);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get booking details", description = "Get detailed information about a specific booking")
    public ResponseEntity<BookingResponseDto> getBooking(@PathVariable Long bookingId) {
        BookingResponseDto booking = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/{resourceId}/price-rules")
    @Operation(summary = "Get enabled price rules", description = "Get all enabled pricing rules for a resource")
    public ResponseEntity<List<ResourcePriceRuleDto>> getPriceRules(@PathVariable Long resourceId) {
        List<ResourcePriceRuleDto> rules = pricingService.getPriceRulesForResource(resourceId);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/{resourceId}/config")
    @Operation(summary = "Get slot configuration", description = "Get the slot configuration for a resource")
    public ResponseEntity<ResourceSlotConfigDto> getSlotConfig(@PathVariable Long resourceId) {
        ResourceSlotConfigDto config = resourceSlotService.getSlotConfig(resourceId);
        return ResponseEntity.ok(config);
    }

    // ==================== Slot Configuration Management ====================

    @PostMapping("/resources/slot-config")
    @Operation(summary = "Create/Update slot configuration",
               description = "Create or update slot configuration for a resource (opening time, closing time, slot duration, base price)")
    public ResponseEntity<ResourceSlotConfigDto> createOrUpdateSlotConfig(
            @Valid @RequestBody ResourceSlotConfigRequest request) {
        ResourceSlotConfigDto config = resourceSlotService.createOrUpdateSlotConfig(request);
        return ResponseEntity.ok(config);
    }

    // ==================== Price Rules Management ====================

    @PostMapping("/resources/price-rules")
    @Operation(summary = "Add price rule",
               description = "Add a dynamic pricing rule for a resource (e.g., night lighting charges, peak hours, weekend pricing)")
    public ResponseEntity<ResourcePriceRuleDto> addPriceRule(
            @Valid @RequestBody ResourcePriceRuleRequest request) {
        ResourcePriceRuleDto rule = pricingService.createPriceRule(request);
        return ResponseEntity.ok(rule);
    }

    @PostMapping("/resources/price-rules/{ruleId}")
    @Operation(summary = "Update price rule",
               description = "Update an existing pricing rule (time range, day type, price, etc.)")
    public ResponseEntity<ResourcePriceRuleDto> updatePriceRule(
            @PathVariable Long ruleId,
            @Valid @RequestBody ResourcePriceRuleRequest request) {
        ResourcePriceRuleDto updatedRule = pricingService.updateRuleById(ruleId, request);
        return ResponseEntity.ok(updatedRule);
    }

    @DeleteMapping("/resources/price-rules/{ruleId}")
    @Operation(summary = "Delete price rule",
               description = "Delete a pricing rule permanently")
    public ResponseEntity<String> deletePriceRule(@PathVariable Long ruleId) {
        pricingService.deletePriceRule(ruleId);
        return ResponseEntity.ok("Price rule deleted successfully");
    }

    @PatchMapping("/resources/price-rules/{ruleId}/enable")
    @Operation(summary = "Enable price rule",
               description = "Enable a disabled pricing rule")
    public ResponseEntity<ResourcePriceRuleDto> enablePriceRule(@PathVariable Long ruleId) {
        ResourcePriceRuleDto rule = pricingService.togglePriceRule(ruleId, true);
        return ResponseEntity.ok(rule);
    }

    @PatchMapping("/resources/price-rules/{ruleId}/disable")
    @Operation(summary = "Disable price rule",
               description = "Disable a pricing rule without deleting it")
    public ResponseEntity<ResourcePriceRuleDto> disablePriceRule(@PathVariable Long ruleId) {
        ResourcePriceRuleDto rule = pricingService.togglePriceRule(ruleId, false);
        return ResponseEntity.ok(rule);
    }

    // ==================== Revenue Reporting ====================

    @GetMapping("/admins/{adminId}/revenue")
    @Operation(summary = "Get admin revenue report",
               description = "Get complete revenue report for an admin with service-wise and resource-wise breakdown. " +
                       "Shows total revenue, booking count, and average revenue per booking. " +
                       "Only counts CONFIRMED and COMPLETED bookings.")
    public ResponseEntity<AdminRevenueReportDto> getAdminRevenueReport(@PathVariable Long adminId) {
        AdminRevenueReportDto report = revenueService.getAdminRevenueReport(adminId);
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

    // ==================== Activity Management ====================

    @GetMapping("/activities")
    @Operation(summary = "Get all activities", description = "Get all activities (enabled and disabled)")
    public ResponseEntity<List<GetActivityDto>> getActivities() {
        List<GetActivityDto> activities = activityService.getAllActivities();
        return ResponseEntity.ok(activities);
    }

    @PostMapping("/activities")
    @Operation(summary = "Create activity", description = "Create a new activity (e.g., FOOTBALL, CRICKET). Code will be auto-uppercased.")
    public ResponseEntity<GetActivityDto> createActivity(@Valid @RequestBody CreateActivityDto request) {
        GetActivityDto activity = activityService.createActivity(request);
        return ResponseEntity.ok(activity);
    }

    @PutMapping("/activities/{id}")
    @Operation(summary = "Update activity", description = "Update an existing activity's code and name")
    public ResponseEntity<GetActivityDto> updateActivity(
            @PathVariable Long id,
            @Valid @RequestBody CreateActivityDto request) {
        GetActivityDto activity = activityService.updateActivity(id, request);
        return ResponseEntity.ok(activity);
    }

    @DeleteMapping("/activities/{id}")
    @Operation(summary = "Delete activity", description = "Permanently delete an activity")
    public ResponseEntity<String> deleteActivity(@PathVariable Long id) {
        activityService.deleteActivity(id);
        return ResponseEntity.ok("Activity deleted successfully");
    }

    @PatchMapping("/activities/{id}/enable")
    @Operation(summary = "Enable activity", description = "Enable a disabled activity")
    public ResponseEntity<GetActivityDto> enableActivity(@PathVariable Long id) {
        GetActivityDto activity = activityService.enableActivity(id);
        return ResponseEntity.ok(activity);
    }

    @PatchMapping("/activities/{id}/disable")
    @Operation(summary = "Disable activity", description = "Disable an activity without deleting it")
    public ResponseEntity<GetActivityDto> disableActivity(@PathVariable Long id) {
        GetActivityDto activity = activityService.disableActivity(id);
        return ResponseEntity.ok(activity);
    }

    // ==================== Financial Management ====================

    @GetMapping("/finance/summary")
    @Operation(summary = "Global financial summary (all admins)",
               description = "Returns aggregate financial totals across ALL admins: total pending, total settled, " +
                       "total cash/bank balances, total platform-collected online amounts. " +
                       "Use as the manager's top-level financial dashboard widget.")
    public ResponseEntity<ManagerGlobalFinancialSummaryDto> getManagerGlobalFinancialSummary() {
        return ResponseEntity.ok(adminFinancialService.getManagerGlobalSummary());
    }

    @GetMapping("/admins/due-summary")
    @Operation(summary = "Get all admin due summary",
               description = "Returns a list of all admins with their pending online amounts (platform owes them) " +
                       "and total settled amounts. Used by manager to decide who needs to be settled.")
    public ResponseEntity<List<AdminDueSummaryDto>> getAllAdminDueSummary() {
        List<AdminDueSummaryDto> summary = adminFinancialService.getAllAdminDueSummary();
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/admins/pending-due-summary")
    @Operation(summary = "Get admins with pending balance only",
               description = "Returns only admins that currently have a non-zero pendingOnlineAmount. " +
                       "Useful to quickly identify which admins need to be settled.")
    public ResponseEntity<List<AdminDueSummaryDto>> getAdminsWithPendingBalance() {
        return ResponseEntity.ok(adminFinancialService.getAllAdminDueSummaryEnhanced());
    }

    @GetMapping("/admin/{id}/finance")
    @Operation(summary = "Get admin financial overview",
               description = "Returns the complete financial breakdown for a specific admin: " +
                       "cash balance, bank balance, pending online amount, and all lifetime totals.")
    public ResponseEntity<AdminFinancialOverviewDto> getAdminFinancialOverview(@PathVariable Long id) {
        AdminFinancialOverviewDto overview = adminFinancialService.getAdminFinancialOverview(id);
        return ResponseEntity.ok(overview);
    }

    @PostMapping("/admin/{id}/settle")
    @Operation(summary = "Settle amount to admin",
               description = "Transfer pending online amount to admin's bank. " +
                       "Cannot exceed the admin's current pendingOnlineAmount. " +
                       "Updates: pendingOnlineAmount--, bankBalance++, totalSettledAmount++. " +
                       "Creates an immutable Settlement record and FinancialTransaction audit entry. " +
                       "Accepts optional notes/remarks for the settlement record.")
    public ResponseEntity<SettlementDto> settleAmount(
            @PathVariable Long id,
            @Valid @RequestBody SettleRequest request) {
        SettlementDto settlement = adminFinancialService.settleAmount(
                id,
                request.getAmount(),
                request.getPaymentMode(),
                request.getSettlementReference(),
                request.getNotes()
        );
        return ResponseEntity.ok(settlement);
    }

    @GetMapping("/settlements/{settlementId}")
    @Operation(summary = "Get settlement by ID",
               description = "Fetch a specific settlement record by its ID.")
    public ResponseEntity<SettlementDto> getSettlementById(@PathVariable Long settlementId) {
        return ResponseEntity.ok(adminFinancialService.getSettlementById(settlementId));
    }

    @PatchMapping("/settlements/{settlementId}/notes")
    @Operation(summary = "Update settlement notes",
               description = "Add or update the notes/remarks on an existing settlement record. " +
                       "Useful for recording the UTR, bank reference, or any follow-up remarks.")
    public ResponseEntity<SettlementDto> updateSettlementNotes(
            @PathVariable Long settlementId,
            @RequestParam String notes) {
        return ResponseEntity.ok(adminFinancialService.updateSettlementNotes(settlementId, notes));
    }

    // ─── Manager Settlement Ledger ────────────────────────────────────────────

    @GetMapping("/finance/settlement-ledger")
    @Operation(summary = "Manager global settlement ledger",
               description = "Complete paginated ledger of ALL settlements across ALL admins, newest first. " +
                       "Optionally filter by date range using `from` and `to` ISO-8601 timestamps. " +
                       "Example: ?from=2025-01-01T00:00:00Z&to=2025-12-31T23:59:59Z")
    public ResponseEntity<PaginatedResponse<ManagerSettlementLedgerEntryDto>> getManagerSettlementLedger(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                adminFinancialService.getManagerSettlementLedger(from, to, page, size));
    }

    @GetMapping("/admins/{adminId}/settlement-ledger")
    @Operation(summary = "Admin settlement ledger (manager view)",
               description = "Complete paginated settlement history for a specific admin as a rich ledger entry " +
                       "(includes pendingAfter snapshot and notes). " +
                       "Optionally filter by date range using `from` and `to` ISO-8601 timestamps.")
    public ResponseEntity<PaginatedResponse<ManagerSettlementLedgerEntryDto>> getAdminSettlementLedger(
            @PathVariable Long adminId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                adminFinancialService.getAdminSettlementLedger(adminId, from, to, page, size));
    }

    // ─── Admin Ledger Endpoints ───────────────────────────────────────────────

    @GetMapping("/admins/{adminId}/ledger")
    @Operation(summary = "Get admin combined ledger (CASH + BANK) - manager view",
               description = "Returns a unified paginated ledger combining both CASH and BANK entries for a specific admin, " +
                       "ordered newest first. Optionally filter by date range using ISO-8601 `from`/`to` timestamps.")
    public ResponseEntity<PaginatedResponse<AdminLedgerEntryDto>> getAdminCombinedLedger(
            @PathVariable Long adminId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                adminFinancialService.getAdminCombinedLedger(adminId, from, to, page, size));
    }

    @GetMapping("/admins/{adminId}/ledger/cash")
    @Operation(summary = "Get admin cash ledger (manager view)",
            description = "Manager can view a specific admin's CASH ledger history (credits and debits with running balance). " +
                    "Optionally filter by date range using ISO-8601 `from`/`to` timestamps.")
    public ResponseEntity<PaginatedResponse<AdminLedgerEntryDto>> getAdminCashLedger(
            @PathVariable Long adminId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                adminFinancialService.getAdminLedgerWithDateRange(adminId, AdminLedgerType.CASH, from, to, page, size));
    }

    @GetMapping("/admins/{adminId}/ledger/bank")
    @Operation(summary = "Get admin bank ledger (manager view)",
            description = "Manager can view a specific admin's BANK ledger history (credits and debits with running balance). " +
                    "Optionally filter by date range using ISO-8601 `from`/`to` timestamps.")
    public ResponseEntity<PaginatedResponse<AdminLedgerEntryDto>> getAdminBankLedger(
            @PathVariable Long adminId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                adminFinancialService.getAdminLedgerWithDateRange(adminId, AdminLedgerType.BANK, from, to, page, size));
    }

    @GetMapping("/admins/{adminId}/settlements")
    @Operation(summary = "Get admin settlement history (manager view)",
            description = "Manager can view the complete settlement history for a specific admin.")
    public ResponseEntity<PaginatedResponse<SettlementDto>> getAdminSettlements(
            @PathVariable Long adminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                adminFinancialService.getSettlementHistory(adminId, page, size));
    }

    @GetMapping("/admins/{adminId}/transactions")
    @Operation(summary = "Get admin transaction history (manager view)",
            description = "Manager can view the complete financial transaction audit log for a specific admin.")
    public ResponseEntity<PaginatedResponse<FinancialTransactionDto>> getAdminTransactions(
            @PathVariable Long adminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                adminFinancialService.getTransactionHistory(adminId, page, size));
    }

    // ─── App Config CRUD ───────────────────────────────────────────────────────

    @GetMapping("/app-config")
    @Operation(summary = "List all app configs", description = "Retrieve all app update configuration records")
    public ResponseEntity<List<AppConfigResponse>> getAllAppConfigs() {
        return ResponseEntity.ok(appConfigService.getAllConfigs());
    }

    @GetMapping("/app-config/{id}")
    @Operation(summary = "Get app config by ID", description = "Retrieve a specific app update configuration record by its ID")
    public ResponseEntity<AppConfigResponse> getAppConfigById(@PathVariable Long id) {
        return ResponseEntity.ok(appConfigService.getConfigById(id));
    }

    @PostMapping("/app-config")
    @Operation(summary = "Create app config", description = "Create a new app update configuration record")
    public ResponseEntity<AppConfigResponse> createAppConfig(@Valid @RequestBody AppConfigRequest request) {
        return ResponseEntity.ok(appConfigService.createConfig(request));
    }

    @PutMapping("/app-config/{id}")
    @Operation(summary = "Update app config", description = "Update an existing app update configuration record by its ID")
    public ResponseEntity<AppConfigResponse> updateAppConfig(
            @PathVariable Long id,
            @Valid @RequestBody AppConfigRequest request) {
        return ResponseEntity.ok(appConfigService.updateConfig(id, request));
    }

    @DeleteMapping("/app-config/{id}")
    @Operation(summary = "Delete app config", description = "Delete an app update configuration record by its ID")
    public ResponseEntity<Void> deleteAppConfig(@PathVariable Long id) {
        appConfigService.deleteConfig(id);
        return ResponseEntity.noContent().build();
    }
}
