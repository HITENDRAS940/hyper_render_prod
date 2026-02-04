package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.entity.User;
import com.hitendra.turf_booking_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for user registration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

    private final UserRepository userRepository;

    /**
     * Registers a new user.
     *
     * @param phone The phone number of the user
     * @return The created User entity
     * @throws IllegalStateException if user already exists
     */
    @Transactional
    public User registerNewUser(String phone) {
        // Safety check: Ensure no user exists with this phone (optimized)
        if (userRepository.existsByPhone(phone)) {
            throw new IllegalStateException("User already exists with phone: " + phone);
        }

        // Generate email from phone (temporary email for phone-only users)
        // Format: phone_<phoneNumber>@temp.hyper.com
        String tempEmail = "phone_" + phone.replaceAll("[^0-9]", "") + "@temp.hyper.com";

        // Create and persist the user
        User user = User.builder()
                .phone(phone)
                .email(tempEmail)
                .build();
        User savedUser = userRepository.save(user);

        log.info("Created new user: userId={}, phone={}, email={}", savedUser.getId(), phone, tempEmail);

        return savedUser;
    }

    /**
     * Check if a user exists by phone number (optimized).
     *
     * @param phone The phone number to check
     * @return true if user exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean userExists(String phone) {
        return userRepository.existsByPhone(phone);
    }
}
