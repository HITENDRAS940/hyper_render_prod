package com.hitendra.turf_booking_backend.dto.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

/**
 * Complete detail view of a service — for manager use only.
 * Includes every field of the service plus the full detail of each resource
 * (slot config + price rules included per resource).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDetailDto {

    // ── Identity ──────────────────────────────────────────────────────────
    private Long id;
    private String name;

    // ── Location ──────────────────────────────────────────────────────────
    private String location;
    private String city;
    private String state;
    private Double latitude;
    private Double longitude;

    // ── Details ───────────────────────────────────────────────────────────
    private String description;
    private String contactNumber;
    private String gstin;
    private LocalTime startTime;
    private LocalTime endTime;

    // ── Flags ─────────────────────────────────────────────────────────────
    private boolean availability;
    private boolean refundAllowed;

    // ── Media & taxonomy ─────────────────────────────────────────────────
    private List<String> amenities;
    private List<String> images;
    private List<String> activities;

    // ── Google Places ──────────────────────────────────────────────────────
    private String googlePlaceId;
    private Double googleRating;
    private Integer googleReviewCount;

    // ── Ownership ─────────────────────────────────────────────────────────
    private Long createdByAdminId;
    private String createdByAdminName;

    // ── Resources (full detail each) ──────────────────────────────────────
    private List<ResourceDetailDto> resources;
}

