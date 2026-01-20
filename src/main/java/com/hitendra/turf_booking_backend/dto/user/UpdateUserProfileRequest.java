package com.hitendra.turf_booking_backend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileRequest {
    private String name;
    private String email;
    private String address;
    private LocalDate dateOfBirth;
    private String city;
    private String state;
    private String pincode;
}

