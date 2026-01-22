package com.hitendra.turf_booking_backend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteProfileRequest {
    private String reason;  // Optional reason for deletion
    private String confirmationText;  // User must type "DELETE" to confirm
}
