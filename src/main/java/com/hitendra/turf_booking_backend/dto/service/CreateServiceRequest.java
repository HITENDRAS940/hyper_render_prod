package com.hitendra.turf_booking_backend.dto.service;

import com.hitendra.turf_booking_backend.entity.ResourceSelectionMode;
import lombok.Data;
import java.util.List;

@Data
public class CreateServiceRequest {
    private String name;
    private String location;
    private String city;
    private Double latitude;
    private Double longitude;
    private String description;
    private String contactNumber;
    private List<String> activityCodes;  // List of activity codes (e.g., ["CRICKET", "FOOTBALL"])
    private List<String> amenities;      // List of amenities (e.g., ["Parking", "WiFi", "Cafeteria", "Lighting"])

    /**
     * Whether user cancellation (with refund) is permitted for bookings under this service.
     * Defaults to true when not provided.
     */
    private Boolean refundAllowed = true;

    /**
     * Percentage of total booking amount to be paid online (rest collected at venue).
     * When null the global pricing.online-payment-percent config value is used as fallback.
     * Valid range: 0 – 100.
     */
    private Double onlinePaymentPercent;

    /**
     * Resource selection mode for this service.
     *
     * AUTO (default): Backend allocates resource using priority algorithm.
     *   - Suitable for: Turfs, courts where resources are interchangeable
     *   - Frontend shows aggregated slot availability
     *
     * MANUAL: User explicitly selects which specific resource to book.
     *   - Suitable for: Bowling lanes, arcade machines where resources have distinct characteristics
     *   - Frontend shows individual resource slot availability
     *
     * Defaults to AUTO when not provided for backward compatibility.
     */
    private ResourceSelectionMode resourceSelectionMode;

    /**
     * Google Places place_id for this service/venue.
     * Used to fetch rating and review count from Google Places API.
     */
    private String googlePlaceId;
}

