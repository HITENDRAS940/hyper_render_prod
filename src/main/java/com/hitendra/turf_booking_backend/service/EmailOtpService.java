package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.auth.JwtResponseDto;
import com.hitendra.turf_booking_backend.dto.auth.RequestEmailOtpDto;
import com.hitendra.turf_booking_backend.dto.auth.VerifyEmailOtpDto;
import com.hitendra.turf_booking_backend.entity.Otp;
import com.hitendra.turf_booking_backend.entity.User;
import com.hitendra.turf_booking_backend.exception.AuthException;
import com.hitendra.turf_booking_backend.repository.OtpRepository;
import com.hitendra.turf_booking_backend.repository.UserRepository;
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
public class EmailOtpService {

    private final UserRepository userRepository;
    private final OtpRepository otpRepository;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;

    /**
     * Request OTP for email
     */
    public void requestEmailOtp(RequestEmailOtpDto request) {
        String email = request.getEmail().toLowerCase().trim();

        // Generate 6-digit OTP
        String otpCode = generateOtpCode(email);

        log.info("Generating OTP for email: {}", email);

        // Save OTP to database
        Otp otp = Otp.builder()
                .email(email)
                .code(otpCode)
                .expiresAt(Instant.now().plusSeconds(300)) // 5 minutes
                .build();

        otpRepository.save(otp);

        // Send OTP via email
        try {
            emailService.sendOtpEmail(email, otpCode);
            log.info("OTP email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", email, e);
            throw new AuthException(
                AuthException.AuthErrorCode.INVALID_OTP,
                "Failed to send OTP email. Please try again."
            );
        }
    }

    /**
     * Verify email OTP and return JWT
     */
    public JwtResponseDto verifyEmailOtp(VerifyEmailOtpDto request) {
        String otpCode = request.getOtp();
        String email = request.getEmail().toLowerCase().trim();

        log.info("Verifying OTP for email: {}", email);

        // Find the latest unused OTP for this email
        Optional<Otp> otpOpt = otpRepository.findTopByEmailAndUsedFalseOrderByExpiresAtDesc(email);

        // Check if OTP exists
        if (otpOpt.isEmpty()) {
            log.error("OTP not found for email: {}", email);
            throw new AuthException(
                AuthException.AuthErrorCode.OTP_NOT_FOUND,
                "No OTP found for this email. Please request a new OTP."
            );
        }

        Otp otp = otpOpt.get();

        // Check if OTP has expired
        if (otp.getExpiresAt().isBefore(Instant.now())) {
            log.error("OTP expired for email: {}", email);
            throw new AuthException(
                AuthException.AuthErrorCode.EXPIRED_OTP,
                "The OTP has expired. Please request a new OTP."
            );
        }

        // Check if OTP code matches
        if (!otp.getCode().equals(otpCode)) {
            log.error("Invalid OTP for email: {}. Expected: {}, Got: {}", email, otp.getCode(), otpCode);
            throw new AuthException(
                AuthException.AuthErrorCode.INVALID_OTP,
                "The OTP entered is incorrect or expired"
            );
        }

        // Mark OTP as used
        otp.setUsed(true);
        otpRepository.save(otp);

        // Check if user exists (login) or needs to be created (registration)
        User user = userRepository.findByEmail(email).orElse(null);
        boolean isNewUser = false;

        if (user == null) {
            // REGISTRATION FLOW: Create new user
            log.info("New user registration via email: {}", email);

            user = User.builder()
                    .email(email)
                    .emailVerified(true)
                    .build();

            user = userRepository.save(user);


            isNewUser = true;
        } else {
            // LOGIN FLOW: User already exists
            log.info("Existing user login via email: userId={}, email={}", user.getId(), email);

            // Update email verified status if not already set
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                userRepository.save(user);
            }

            isNewUser = false;
        }

        // Generate JWT token (email-based)
        String token = jwtUtils.generateTokenFromEmail(
                user.getEmail(),
                "ROLE_" + user.getRole().name(),
                user.getId(),
                user.getName(),
                user.getPhone()
        );

        log.info("Email OTP verification successful for user: {}", user.getId());

        return new JwtResponseDto(token, isNewUser);
    }

    /**
     * Generate 6-digit OTP code
     */
    private String generateOtpCode(String email) {
        // For testing: return "000000" for any admin email (*.admin@hyper.com)
        // Examples: vellore.admin@hyper.com, mumbai.admin@hyper.com, etc.
        if (email.toLowerCase().endsWith("@hyper.com")) {
            log.info("Using hardcoded OTP '000000' for admin/test email: {}", email);
            return "000000";
        }

        // For production users, use random OTP
        return String.format("%06d", new Random().nextInt(1000000));
    }
}

