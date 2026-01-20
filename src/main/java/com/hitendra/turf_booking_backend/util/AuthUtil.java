package com.hitendra.turf_booking_backend.util;

import com.hitendra.turf_booking_backend.entity.AdminProfile;
import com.hitendra.turf_booking_backend.entity.Role;
import com.hitendra.turf_booking_backend.entity.User;
import com.hitendra.turf_booking_backend.repository.AdminProfileRepository;
import com.hitendra.turf_booking_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthUtil {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdminProfileRepository adminProfileRepository;

    private String getEmailFromAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    public User getCurrentUser() {
        return userRepository.findUserByEmail(getEmailFromAuth()).orElse(null);
    }

    public Long getCurrentUserId() {
        User user = getCurrentUser();
        if (user == null) {
            throw new RuntimeException("User not authenticated");
        }
        return user.getId();
    }

    public AdminProfile getCurrentAdminProfile() {

        User user = userRepository.findByEmail(getEmailFromAuth())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            throw new RuntimeException("Only admins can create turfs");
        }

        // For managers, we might want to create a default admin profile or handle differently
        // For now, only admins with profiles can create turfs
        if (user.getRole() == Role.ADMIN) {
            return adminProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Admin profile not found. Please complete your profile first."));
        } else {
            // Manager case - they can create turfs but we need to handle this
            throw new RuntimeException("Managers should delegate turf creation to admins");
        }
    }


}
