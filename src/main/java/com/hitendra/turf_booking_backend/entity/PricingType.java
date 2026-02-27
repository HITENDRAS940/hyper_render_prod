package com.hitendra.turf_booking_backend.entity;

/**
 * Defines the pricing strategy for a ServiceResource.
 *
 * PER_SLOT  - Price is a flat rate per slot regardless of headcount.
 *             Example: Turf booking — doesn't matter how many people are playing.
 *
 * PER_PERSON - Price is multiplied by the number of persons.
 *              Example: PS5 gaming, Bowling, VR — each person pays individually.
 */
public enum PricingType {
    PER_SLOT,
    PER_PERSON
}

