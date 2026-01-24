package com.hitendra.turf_booking_backend.dto.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A lightweight DTO for admin service listing.
 * Contains only essential fields: id, name, location, city, and availability.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminServiceSummaryDto {
    private Long id;
    private String name;
    private String location;
    private String city;
    private boolean availability;
}
