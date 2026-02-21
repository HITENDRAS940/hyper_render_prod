package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.location.DistanceCalculationRequest;
import com.hitendra.turf_booking_backend.dto.location.DistanceCalculationResponse;
import com.hitendra.turf_booking_backend.dto.location.FilterServicesByDistanceRequest;
import com.hitendra.turf_booking_backend.dto.service.ServiceCardDto;
import com.hitendra.turf_booking_backend.entity.Service;
import com.hitendra.turf_booking_backend.repository.ServiceRepository;
import com.hitendra.turf_booking_backend.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Public controller for location-related operations
 * No authentication required
 */
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Location Services", description = "Public APIs for location calculations")
public class LocationController {

    private final LocationService locationService;
    private final ServiceRepository serviceRepository;

    /**
     * Calculate distance between user location and a service location
     *
     * DESCRIPTION:
     * This endpoint calculates the straight-line distance (as the crow flies)
     * between user's current location and a service location using the Haversine formula.
     * Service coordinates are fetched from the database using the provided serviceId.
     *
     * PARAMETERS:
     * - serviceId: ID of the service to calculate distance to
     * - userLatitude: User's current latitude
     * - userLongitude: User's current longitude
     *
     * RESPONSE:
     * - distanceInKilometers: Distance in kilometers
     * - distanceInMeters: Distance in meters
     * - distanceInMiles: Distance in miles
     * - formattedDistance: Human-readable formatted distance (e.g., "2.5 km")
     *
     * EXAMPLE REQUEST:
     * {
     *   "serviceId": 1,
     *   "userLatitude": 28.7041,
     *   "userLongitude": 77.1025
     * }
     *
     * EXAMPLE RESPONSE:
     * {
     *   "distanceInKilometers": 15.45,
     *   "distanceInMeters": 15450.00,
     *   "distanceInMiles": 9.60,
     *   "formattedDistance": "15.45 km"
     * }
     *
     * ERROR CASES:
     * - Service not found: Returns 404
     * - Invalid coordinates: Returns 400
     * - Service has no location data: Returns 400
     */
    @PostMapping("/calculate-distance")
    @Operation(summary = "Calculate distance to a service",
               description = "Calculate the distance between user's current location and a service location. " +
                       "Service coordinates are automatically fetched from the database using the provided serviceId. " +
                       "Returns distance in kilometers, meters, and miles.")
    public ResponseEntity<DistanceCalculationResponse> calculateDistanceToService(
            @Valid @RequestBody DistanceCalculationRequest request
    ) {
        log.info("Calculating distance to service {} from user location ({}, {})",
                request.getServiceId(), request.getUserLatitude(), request.getUserLongitude());

        // Validate user coordinates
        validateCoordinates(request.getUserLatitude(), request.getUserLongitude(), "user");

        // Fetch service from database
        Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> {
                    log.warn("Service not found with ID: {}", request.getServiceId());
                    return new RuntimeException("Service not found with ID: " + request.getServiceId());
                });

        // Validate service has location data
        if (service.getLatitude() == null || service.getLongitude() == null) {
            log.warn("Service {} does not have valid location data", request.getServiceId());
            throw new RuntimeException("Service does not have valid location data. Latitude: " +
                    service.getLatitude() + ", Longitude: " + service.getLongitude());
        }

        log.debug("Service {} location: ({}, {})", request.getServiceId(), service.getLatitude(), service.getLongitude());

        // Validate service coordinates
        validateCoordinates(service.getLatitude(), service.getLongitude(), "service");

        // Calculate distance in kilometers using Haversine formula
        double distanceInKm = locationService.calculateDistance(
                request.getUserLatitude(),
                request.getUserLongitude(),
                service.getLatitude(),
                service.getLongitude()
        );

        log.info("Distance calculated: {} km to service {}", distanceInKm, request.getServiceId());

        // Convert to meters and miles
        double distanceInMeters = distanceInKm * 1000;
        double distanceInMiles = distanceInKm * 0.621371;

        // Format the distance for display
        String formattedDistance = formatDistance(distanceInKm);

        // Build response
        DistanceCalculationResponse response = DistanceCalculationResponse.builder()
                .distanceInKilometers(Math.round(distanceInKm * 100.0) / 100.0)
                .distanceInMeters(Math.round(distanceInMeters * 100.0) / 100.0)
                .distanceInMiles(Math.round(distanceInMiles * 100.0) / 100.0)
                .formattedDistance(formattedDistance)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Validate latitude and longitude coordinates
     * Latitude: -90 to 90
     * Longitude: -180 to 180
     */
    private void validateCoordinates(Double latitude, Double longitude, String locationType) {
        if (latitude == null || longitude == null) {
            throw new RuntimeException(locationType + " latitude and longitude cannot be null");
        }

        if (latitude < -90 || latitude > 90) {
            throw new RuntimeException("Invalid " + locationType + " latitude: " + latitude + ". Must be between -90 and 90");
        }

        if (longitude < -180 || longitude > 180) {
            throw new RuntimeException("Invalid " + locationType + " longitude: " + longitude + ". Must be between -180 and 180");
        }
    }

    /**
     * Format distance for human-readable display
     * Shows kilometers for distances >= 1 km
     * Shows meters for distances < 1 km
     */
    private String formatDistance(double distanceInKm) {
        if (distanceInKm >= 1) {
            return String.format("%.2f km", distanceInKm);
        } else {
            double distanceInMeters = distanceInKm * 1000;
            return String.format("%.0f m", distanceInMeters);
        }
    }

    // ==================== FILTER SERVICES BY DISTANCE ====================

    /**
     * Filter services by distance from user location
     *
     * DESCRIPTION:
     * This endpoint finds all services within a specified distance range from the user's location.
     * Services are sorted by distance (nearest first).
     *
     * PARAMETERS:
     * - userLatitude: User's current latitude
     * - userLongitude: User's current longitude
     * - maxDistanceKm: Maximum distance in kilometers (e.g., 5, 10, 15)
     * - minDistanceKm (optional): Minimum distance in kilometers (default: 0)
     * - city (optional): Filter by city name
     *
     * RESPONSE:
     * Returns list of services with:
     * - Service basic info (id, name, city, location, description)
     * - Distance in kilometers, meters, and miles
     * - Formatted distance for UI display
     * - Sorted by distance (nearest first)
     *
     * EXAMPLE REQUEST:
     * {
     *   "userLatitude": 24.5854,
     *   "userLongitude": 73.7125,
     *   "maxDistanceKm": 10,
     *   "minDistanceKm": 0,
     *   "city": "Udaipur"
     * }
     *
     * EXAMPLE RESPONSE:
     * [
     *   {
     *     "serviceId": 1,
     *     "serviceName": "Lake City Football Arena",
     *     "city": "Udaipur",
     *     "location": "Sajjangarh Road, Udaipur",
     *     "description": "Premium 5v5 and 7v7 football turf with night floodlights",
     *     "distanceInKilometers": 0.85,
     *     "distanceInMeters": 850.0,
     *     "distanceInMiles": 0.53,
     *     "formattedDistance": "0.85 km"
     *   },
     *   {
     *     "serviceId": 2,
     *     "serviceName": "Royal City Sports Ground",
     *     "city": "Udaipur",
     *     "location": "Fateh Sagar Road, Udaipur",
     *     "description": "Full-size football ground with synthetic turf",
     *     "distanceInKilometers": 2.34,
     *     "distanceInMeters": 2340.0,
     *     "distanceInMiles": 1.45,
     *     "formattedDistance": "2.34 km"
     *   }
     * ]
     *
     * ERROR CASES:
     * - Invalid coordinates: Returns 400
     * - Invalid distance range: Returns 400
     * - No services found: Returns empty list with 200 OK
     */
    @PostMapping("/filter-services-by-distance")
    @Operation(summary = "Filter services by distance",
               description = "Find all services within a specified distance from user's location. " +
                       "Returns services sorted by distance (nearest first). " +
                       "Optionally filter by city. " +
                       "Distance information is calculated using Haversine formula.")
    public ResponseEntity<List<ServiceCardDto>> filterServicesByDistance(
            @Valid @RequestBody FilterServicesByDistanceRequest request
    ) {
        log.info("Filtering services by distance: maxDistance={}km, minDistance={}km, city={}, userLocation=({}, {})",
                request.getMaxDistanceKm(), request.getMinDistanceKm(), request.getCity(),
                request.getUserLatitude(), request.getUserLongitude());

        // Validate user coordinates
        validateCoordinates(request.getUserLatitude(), request.getUserLongitude(), "user");

        // Validate distance range
        if (request.getMaxDistanceKm() == null || request.getMaxDistanceKm() <= 0) {
            throw new RuntimeException("Max distance must be greater than 0");
        }

        Double minDistance = request.getMinDistanceKm() != null ? request.getMinDistanceKm() : 0.0;
        if (minDistance < 0) {
            throw new RuntimeException("Min distance cannot be negative");
        }

        if (minDistance > request.getMaxDistanceKm()) {
            throw new RuntimeException("Min distance cannot be greater than max distance");
        }

        log.debug("Distance range: {} to {} km", minDistance, request.getMaxDistanceKm());

        // Fetch all services (optionally filtered by city)
        List<Service> services;
        if (request.getCity() != null && !request.getCity().isBlank()) {
            services = serviceRepository.findByCityIgnoreCase(request.getCity());
            log.info("Found {} services in city: {}", services.size(), request.getCity());
        } else {
            services = serviceRepository.findAll();
            log.info("Found {} total services", services.size());
        }

        if (services.isEmpty()) {
            log.info("No services found for filtering");
            return ResponseEntity.ok(new ArrayList<>());
        }

        // Calculate distance for each service and filter by distance range
        List<ServiceCardDto> servicesWithDistance = services.stream()
                .filter(service -> {
                    // Validate service has location data
                    if (service.getLatitude() == null || service.getLongitude() == null) {
                        log.warn("Service {} has no location data, skipping", service.getId());
                        return false;
                    }
                    return true;
                })
                .map(service -> {
                    // Calculate distance
                    double distanceInKm = locationService.calculateDistance(
                            request.getUserLatitude(),
                            request.getUserLongitude(),
                            service.getLatitude(),
                            service.getLongitude()
                    );

                    // Filter by distance range
                    if (distanceInKm < minDistance || distanceInKm > request.getMaxDistanceKm()) {
                        return null;
                    }

                    // Build ServiceCardDto
                    ServiceCardDto cardDto = new ServiceCardDto();
                    cardDto.setId(service.getId());
                    cardDto.setName(service.getName());
                    cardDto.setLocation(service.getLocation());
                    cardDto.setAvailability(service.isAvailability());
                    cardDto.setImages(service.getImages());

                    return cardDto;
                })
                .filter(dto -> dto != null) // Remove null entries (services outside distance range)
                .sorted(Comparator.comparingDouble(dto -> {
                    // Re-calculate distance for sorting (services already filtered by distance)
                    Service service = services.stream()
                            .filter(s -> s.getId().equals(dto.getId()))
                            .findFirst()
                            .orElse(null);
                    if (service == null) return Double.MAX_VALUE;
                    return locationService.calculateDistance(
                            request.getUserLatitude(),
                            request.getUserLongitude(),
                            service.getLatitude(),
                            service.getLongitude()
                    );
                }))
                .collect(Collectors.toList());

        log.info("Filtered {} services within {}km distance range", servicesWithDistance.size(), request.getMaxDistanceKm());

        return ResponseEntity.ok(servicesWithDistance);
    }
}