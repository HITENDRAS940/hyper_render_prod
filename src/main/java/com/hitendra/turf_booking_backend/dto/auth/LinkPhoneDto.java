package com.hitendra.turf_booking_backend.dto.auth;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LinkPhoneDto {
    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    private String email;
    @NotBlank(message = "Phone number is required")
    private String phone;
}
