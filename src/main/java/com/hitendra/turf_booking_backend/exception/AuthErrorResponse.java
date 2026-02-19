package com.hitendra.turf_booking_backend.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthErrorResponse {
    private boolean success;
    private String errorCode;
    private String message;
}

