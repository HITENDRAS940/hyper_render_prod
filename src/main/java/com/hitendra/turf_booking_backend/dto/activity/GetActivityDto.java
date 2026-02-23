package com.hitendra.turf_booking_backend.dto.activity;


import lombok.Data;

@Data
public class GetActivityDto {
    private Long id;
    private String code;
    private String name;
    private boolean enabled;
}
