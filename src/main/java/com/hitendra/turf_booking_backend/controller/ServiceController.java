package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.activity.GetActivityDto;
import com.hitendra.turf_booking_backend.dto.service.ServiceDto;
import com.hitendra.turf_booking_backend.dto.service.ServiceResourceDto;
import com.hitendra.turf_booking_backend.dto.service.ResourceAvailabilityResponseDto;
import com.hitendra.turf_booking_backend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hitendra.turf_booking_backend.dto.service.*;
import com.hitendra.turf_booking_backend.dto.common.PaginatedResponse;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Public services APIs for users")
public class ServiceController {

    private final ServiceService serviceService;
    private final ServiceResourceService serviceResourceService;
    private final ResourceSlotService resourceSlotService;
    private final ActivityService activityService;

    @GetMapping
    @Operation(summary = "List Services", description = "Get all available services with basic card information")
    public ResponseEntity<PaginatedResponse<ServiceCardDto>> getAllServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<ServiceCardDto> turfs = serviceService.getAllServicesCard(page, size);
        return ResponseEntity.ok(turfs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get services details", description = "Get detailed information about a specific service by ID")
    public ResponseEntity<ServiceDto> getService(@PathVariable Long id) {
        ServiceDto service = serviceService.getServiceById(id);
        return ResponseEntity.ok(service);
    }

    @GetMapping("/{id}/lowest-price")
    @Operation(summary = "Get lowest slot price", description = "Get the lowest price among all enabled slots for a specific service")
    public ResponseEntity<Double> getLowestSlotPrice(@PathVariable Long id) {
        Double lowestPrice = serviceService.getLowestSlotPrice(id);
        return ResponseEntity.ok(lowestPrice);
    }


    @GetMapping("/{id}/availability/check")
    @Operation(summary = "Check if service is available", description = "Check if the service itself is enabled and available for booking")
    public ResponseEntity<Boolean> isServiceAvailable(@PathVariable Long id) {
        Boolean available = serviceService.isServiceAvailable(id);
        return ResponseEntity.ok(available);
    }

    @GetMapping("/cities")
    @Operation(summary = "Get available cities", description = "Get list of all cities where services are available")
    public ResponseEntity<List<String>> getAvailableCities() {
        List<String> cities = serviceService.getAvailableCities();
        return ResponseEntity.ok(cities);
    }

    @GetMapping("/by-city")
    @Operation(summary = "Get services by city", description = "Get all services in a specific city")
    public ResponseEntity<PaginatedResponse<ServiceCardDto>> getServicesByCity(
            @RequestParam String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PaginatedResponse<ServiceCardDto> services = serviceService.getServicesCardByCity(city, page, size);
        return ResponseEntity.ok(services);
    }

//    @PostMapping("/by-location")
//    @Operation(summary = "Get services by user location",
//               description = "Get services in the city detected from user's coordinates. Falls back to nearby turfs if city detection fails.")
//    public ResponseEntity<List<ServiceDto>> getServicesByUserLocation(
//            @RequestBody LocationRequest locationRequest
//    ) {
//        List<ServiceDto> services = serviceService.getServicesByUserLocation(
//            locationRequest.getLatitude(),
//            locationRequest.getLongitude()
//        );
//        return ResponseEntity.ok(services);
//    }

//    @PostMapping("/nearby")
//    @Operation(summary = "Get nearby services",
//               description = "Get services near user's location sorted by distance. Optional radius parameter (default 50km)")
//    public ResponseEntity<List<ServiceDto>> getNearbyTurfs(
//            @RequestBody LocationRequest locationRequest,
//            @RequestParam(required = false) Double radiusKm) {
//        List<ServiceDto> serviceDtos = serviceService.getServicesNearLocation(
//            locationRequest.getLatitude(),
//            locationRequest.getLongitude(),
//            radiusKm
//        );
//        return ResponseEntity.ok(serviceDtos);
//    }

    @PostMapping("/detect-city")
    @Operation(summary = "Detect city from coordinates",
               description = "Get city name from latitude and longitude using reverse geocoding")
    public ResponseEntity<CityResponse> detectCity(
            @RequestBody LocationRequest locationRequest) {
        CityResponse city = serviceService.getCityFromCoordinates(
            locationRequest.getLatitude(),
            locationRequest.getLongitude()
        );
        return ResponseEntity.ok(city);
    }

    @GetMapping("/search-by-availability")
    @Operation(summary = "Search services by date and slot availability",
               description = "Find services that have specific slots available on a given date. Optionally filter by city, time range, and activity.")
    public ResponseEntity<List<ServiceSearchDto>> searchServicesByAvailability(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") java.time.LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") java.time.LocalTime endTime,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String activityCode) {
        List<ServiceSearchDto> services = serviceService.searchServicesByAvailability(date, startTime, endTime, city, activityCode);
        return ResponseEntity.ok(services);
    }

    // ==================== Resource Endpoints ====================

    @GetMapping("/{serviceId}/resources")
    @Operation(summary = "Get service resources", description = "Get all available resources (e.g., Turf 1, Court A) for a specific service")
    public ResponseEntity<List<ServiceResourceDto>> getServiceResources(@PathVariable Long serviceId) {
        List<ServiceResourceDto> resources = serviceResourceService.getEnabledResourcesByServiceId(serviceId);
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/resources/{resourceId}")
    @Operation(summary = "Get resource by ID", description = "Get a specific resource by its ID")
    public ResponseEntity<ServiceResourceDto> getResourceById(@PathVariable Long resourceId) {
        ServiceResourceDto resource = serviceResourceService.getResourceById(resourceId);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/resources/{resourceId}/availability")
    @Operation(summary = "Get slot availability for a resource", description = "Get availability status of all slots for a specific resource on a given date")
    public ResponseEntity<ResourceAvailabilityResponseDto> getResourceSlotAvailability(
            @PathVariable Long resourceId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        ResourceAvailabilityResponseDto availability = resourceSlotService.getSlotAvailability(resourceId, date);
        return ResponseEntity.ok(availability);
    }

    @GetMapping("/activity")
    public ResponseEntity<List<GetActivityDto>> getActivity() {
        List<GetActivityDto> activities = activityService.getAllActivities();
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/{activityId}/activity")
    @Operation(summary = "Get services by activity and city", description = "Get all services for a specific activity in a specific city")
    public ResponseEntity<PaginatedResponse<ServiceCardDto>> getServiceActivityId(
            @PathVariable Long activityId,
            @RequestParam(required = true) String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        com.hitendra.turf_booking_backend.entity.Activity activity = activityService.getActivityById(activityId);
        PaginatedResponse<ServiceCardDto> services = serviceService.getServicesByCityAndActivity(city, activity.getCode(), page, size);
        return ResponseEntity.ok(services);
    }

    @GetMapping("/search")
    @Operation(summary = "Search services", description = "Search services by keyword, city, and activity")
    public ResponseEntity<List<ServiceSearchDto>> searchServices(
            @RequestParam String keyword,
            @RequestParam String city,
            @RequestParam(required = false) String activity
    ) {
        List<ServiceSearchDto> services = serviceService.searchServices(keyword, city, activity);
        return ResponseEntity.ok(services);
    }

    @GetMapping("/resources/{resourceId}/slots-by-date")
    @Operation(summary = "Get detailed slots for resource on a date", description = "Get all slots for a resource on a specific date with status (available/booked/disabled) and pricing. Returns minimal info without extra resource/service details.")
    public ResponseEntity<List<ResourceSlotDetailDto>> getSlotsByResourceAndDate(
            @PathVariable Long resourceId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        List<ResourceSlotDetailDto> slots = resourceSlotService.getDetailedSlotsByResourceAndDate(resourceId, date);
        return ResponseEntity.ok(slots);
    }
}
