package com.hitendra.turf_booking_backend.security.service;

import com.hitendra.turf_booking_backend.entity.AdminProfile;
import com.hitendra.turf_booking_backend.repository.ServiceRepository;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("securityService")
@RequiredArgsConstructor
@Slf4j
public class VenueSecurityService {

    private final ServiceRepository serviceRepository;
    private final AuthUtil authUtil;

    /**
     * Check if the current user is an admin of the specified venue.
     */
    @Transactional(readOnly = true)
    public boolean isVenueAdmin(Long venueId) {
        try {
            AdminProfile currentAdmin = authUtil.getCurrentAdminProfile();
            if (currentAdmin == null) {
                return false;
            }

            return serviceRepository.findById(venueId)
                    .map(service -> service.getCreatedBy() != null &&
                                  service.getCreatedBy().getId().equals(currentAdmin.getId()))
                    .orElse(false);
        } catch (Exception e) {
            log.warn("Error checking venue admin permission for venue {}: {}", venueId, e.getMessage());
            return false;
        }
    }
}

