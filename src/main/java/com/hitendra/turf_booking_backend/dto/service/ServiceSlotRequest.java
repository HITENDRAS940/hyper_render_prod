package com.hitendra.turf_booking_backend.dto.service;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ServiceSlotRequest {
    @NotNull
    private Long slotId;

    @NotNull
    private Double price;

    private boolean enabled = true;
}
