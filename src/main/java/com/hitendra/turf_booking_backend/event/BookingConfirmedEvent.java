package com.hitendra.turf_booking_backend.event;

import com.hitendra.turf_booking_backend.entity.Booking;
import lombok.Getter;

@Getter
public class BookingConfirmedEvent {

    private final Booking booking;

    public BookingConfirmedEvent(Booking booking) {
        this.booking = booking;
    }
}

