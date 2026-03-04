package com.hitendra.turf_booking_backend.dto.service;

import com.hitendra.turf_booking_backend.entity.ResourceSelectionMode;
import lombok.Data;
import java.util.List;

@Data
public class ServiceDto {
    private Long id;
    private String name;
    private String location;
    private String city;
    private Double latitude;
    private Double longitude;
    private String description;
    private String contactNumber; // Need to be removed in production
    private boolean availability;
    private boolean refundAllowed;
    /** Per-service online payment percentage. Null means the global config value applies. */
    private Double onlinePaymentPercent;
    /**
     * Resource selection mode for this service.
     * "AUTO" (default): Backend allocates resource using priority algorithm.
     * "MANUAL": User explicitly selects which resource to book.
     */
    private ResourceSelectionMode resourceSelectionMode;
    private  List<String> amenities;
    private List<String> images;
    private List<String> activities;

    // Google Places rating (cached, updated daily)
    private Double googleRating;
    private Integer googleReviewCount;
}
