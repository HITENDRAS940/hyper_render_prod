package com.hitendra.turf_booking_backend.dto.user;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserBasicInfoDto {
    private String name;

    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Phone number must be between 10-15 digits")
    private String phone;
}
