package com.hitendra.turf_booking_backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailOtpResponseDto {
    private String message;
    private String token;
    private Boolean isNewUser;
    private String email;

    // Factory methods for cleaner usage
    public static EmailOtpResponseDto otpSent() {
        return EmailOtpResponseDto.builder()
                .message("OTP sent successfully to your email")
                .build();
    }

    public static EmailOtpResponseDto testAccountLogin(String token, boolean isNewUser, String email) {
        return EmailOtpResponseDto.builder()
                .token(token)
                .isNewUser(isNewUser)
                .email(email)
                .message("Test account - token generated directly")
                .build();
    }
}

