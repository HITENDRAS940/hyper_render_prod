package com.hitendra.turf_booking_backend.entity;

/**
 * Enum to control how resources are selected during booking.
 *
 * Determines the booking flow:
 *   AUTO   → Backend allocates resource using priority algorithm
 *   MANUAL → User explicitly selects resource during booking
 */
public enum ResourceSelectionMode {

    /**
     * Automatic resource allocation (default).
     * Backend uses PRIORITY-BASED allocation:
     *   PRIORITY 1: EXCLUSIVE resources (activity count = 1)
     *   PRIORITY 2: MULTI-ACTIVITY resources (activity count > 1)
     *
     * Use case: Turfs, courts where resources are similar/interchangeable.
     */
    AUTO,

    /**
     * Manual resource selection.
     * User explicitly selects which resource to book via resourceId in the booking request.
     * Backend validates availability for the selected resource only.
     *
     * Use case: Bowling lanes, arcade machines, or any service where
     * resources have distinct characteristics the user cares about.
     */
    MANUAL
}

