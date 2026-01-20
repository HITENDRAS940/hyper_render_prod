package com.hitendra.turf_booking_backend.dto.booking;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AdminBookingRequestDTO {
    @NotNull
    private Long resourceId;

    @NotNull
    private java.time.LocalTime startTime;

    @NotNull
    private java.time.LocalTime endTime;

    @NotNull
    private LocalDate bookingDate;
}
