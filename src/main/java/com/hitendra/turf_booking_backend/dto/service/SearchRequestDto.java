package com.hitendra.turf_booking_backend.dto.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequestDto {
    private LocalDate date;
    private Long slotId;
    private String city;
}
