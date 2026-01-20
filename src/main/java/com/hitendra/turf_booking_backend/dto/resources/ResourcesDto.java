package com.hitendra.turf_booking_backend.dto.resources;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourcesDto {
    private Long serviceId;
    private String serviceName;
    private String location;
    private String latitude;
    private String longitude;
    private String description;
    private List<String> amenities;
    private List<String> images;
    private List<Resources> resources;

    @Data
    @NoArgsConstructor
    static class Resources {
        private Long Id;
        private String name;
        private String description;
        private boolean enabled;
    }
}