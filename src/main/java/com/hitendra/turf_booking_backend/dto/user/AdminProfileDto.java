package com.hitendra.turf_booking_backend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminProfileDto {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String city;
    private String businessName;
    private String businessAddress;
    private String gstNumber;
}

