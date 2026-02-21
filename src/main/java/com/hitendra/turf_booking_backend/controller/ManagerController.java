package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.activity.GetActivityDto;
import com.hitendra.turf_booking_backend.dto.booking.BookingResponseDto;
import com.hitendra.turf_booking_backend.dto.booking.PendingBookingDto;
import com.hitendra.turf_booking_backend.dto.common.PaginatedResponse;
import com.hitendra.turf_booking_backend.dto.revenue.AdminRevenueReportDto;
import com.hitendra.turf_booking_backend.dto.revenue.ServiceRevenueDto;
import com.hitendra.turf_booking_backend.dto.service.*;
import com.hitendra.turf_booking_backend.dto.user.AdminProfileDto;
import com.hitendra.turf_booking_backend.dto.user.CreateAdminRequest;
import com.hitendra.turf_booking_backend.dto.user.UserInfoDto;
import com.hitendra.turf_booking_backend.entity.BookingStatus;
import com.hitendra.turf_booking_backend.repository.ActivityRepository;
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

    @GetMapping("/services")
    @Operation(summary = "Get all the services", description = "Manager can access all the services on the app")
    public ResponseEntity<PaginatedResponse<ServiceCardDto>> getAllServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<ServiceCardDto> services = serviceService.getAllServicesCard(page, size);
        return ResponseEntity.ok(services);
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

    @GetMapping("/activity")
    public ResponseEntity<List<GetActivityDto>> getActivity() {
        List<GetActivityDto> activities = activityService.getAllActivities();
        return ResponseEntity.ok(activities);
    }
}
