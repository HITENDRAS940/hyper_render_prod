package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.auth.*;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.exception.AuthException;
import com.hitendra.turf_booking_backend.repository.*;
import com.hitendra.turf_booking_backend.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final JwtUtils jwtUtils;
    private final SmsService smsService;
    private final UserRegistrationService userRegistrationService;

    public void requestOtp(RequestOtpDto request) {
        String phone = request.getPhone();
//        String otpCode = String.format("%06d", new Random().nextInt(1000000));

        String otpCode = "000000"; // For testing purposes

        Otp otp = Otp.builder()
                .phone(phone)
                .code(otpCode)
                .expiresAt(Instant.now().plusSeconds(300)) // 5 minutes
                .build();

        otpRepository.save(otp);
//        smsService.sendOtp(phone, otpCode);
    }

    public JwtResponseDto verifyOtp(VerifyOtpDto request) {
        String otpCode = request.getOtp();
        String phone = request.getPhone();

        // Find the latest unused OTP for this phone number
        Optional<Otp> otpOpt = otpRepository.findTopByPhoneAndUsedFalseOrderByExpiresAtDesc(phone);

        // Check if OTP exists
        if (otpOpt.isEmpty()) {
            log.error("OTP not found for phone: {}", phone);
            throw new AuthException(
                AuthException.AuthErrorCode.OTP_NOT_FOUND,
                "No OTP found for this phone number. Please request a new OTP."
            );
        }

        Otp otp = otpOpt.get();

        // Check if OTP has expired
        if (otp.getExpiresAt().isBefore(Instant.now())) {
            log.error("OTP expired for phone: {}", phone);
            throw new AuthException(
                AuthException.AuthErrorCode.EXPIRED_OTP,
                "The OTP has expired. Please request a new OTP."
            );
        }

        // Check if OTP code matches
        if (!otp.getCode().equals(otpCode)) {
            log.error("Invalid OTP for phone: {}. Expected: {}, Got: {}", phone, otp.getCode(), otpCode);
            throw new AuthException(
                AuthException.AuthErrorCode.INVALID_OTP,
                "The OTP entered is incorrect or expired"
            );
        }

        // Mark OTP as used
        otp.setUsed(true);
        otpRepository.save(otp);

        // Check if user exists (login flow) or needs to be created (registration flow)
        User user = userRepository.findByPhone(phone).orElse(null);
        boolean isNewUser = false;

        if (user == null) {
            // REGISTRATION FLOW: Create new user
            log.info("New user registration: phone={}", phone);
            user = userRegistrationService.registerNewUser(phone);
            isNewUser = true;
        } else {
            // LOGIN FLOW: User already exists
            log.info("Existing user login: userId={}, phone={}", user.getId(), phone);
            isNewUser = false;
        }

        // Generate token with email (user must have email)
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            throw new AuthException(
                AuthException.AuthErrorCode.USER_NOT_FOUND,
                "User email is required for authentication"
            );
        }

        String token = jwtUtils.generateTokenFromEmail(
                user.getEmail(),
                "ROLE_"+user.getRole().name(),
                user.getId(),
                user.getName()
        );

        return new JwtResponseDto(token, isNewUser);
    }
}
