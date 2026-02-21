package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.booking.BookingResponseDto;
import com.hitendra.turf_booking_backend.dto.service.ServiceDto;
import com.hitendra.turf_booking_backend.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.hitendra.turf_booking_backend.dto.service.*;
import com.hitendra.turf_booking_backend.dto.common.PaginatedResponse;
import com.hitendra.turf_booking_backend.entity.Activity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/services")
@RequiredArgsConstructor
@Tag(name = "Services", description = "Public services APIs for users")
public class ServiceController {

    private final ServiceService serviceService;
    private final ActivityService activityService;
    private final BookingService bookingService;

    @GetMapping("/{id}")
    @Operation(summary = "Get services details", description = "Get detailed information about a specific service by ID")
    public ResponseEntity<ServiceDto> getService(@PathVariable Long id) {
        ServiceDto service = serviceService.getServiceById(id);
        return ResponseEntity.ok(service);
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

    @GetMapping("/search-by-availability")
    @Operation(summary = "Search services by date and slot availability",
               description = "Find services that have specific slots available on a given date. Optionally filter by city, time range, and activity.")
    public ResponseEntity<List<ServiceSearchDto>> searchServicesByAvailability(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String activityCode) {
        List<ServiceSearchDto> services = serviceService.searchServicesByAvailability(date, startTime, endTime, city, activityCode);
        return ResponseEntity.ok(services);
    }

    // ==================== Resource Endpoints ====================

    @GetMapping("/{activityId}/activity")
    @Operation(summary = "Get services by activity and city", description = "Get all services for a specific activity in a specific city")
    public ResponseEntity<PaginatedResponse<ServiceCardDto>> getServiceActivityId(
            @PathVariable Long activityId,
            @RequestParam() String city,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Activity activity = activityService.getActivityById(activityId);
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

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get booking details", description = "Get detailed information about a specific booking")
    public ResponseEntity<BookingResponseDto> getBooking(@PathVariable Long bookingId) {
        BookingResponseDto booking = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(booking);
    }
}
