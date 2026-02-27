package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.service.GoogleRatingResponse;
import com.hitendra.turf_booking_backend.entity.Service;
import com.hitendra.turf_booking_backend.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scheduled job to sync Google Places ratings for all services.
 *
 * Runs daily at 3:00 AM IST (server timezone is set to Asia/Kolkata).
 * Fetches rating and review count from Google Places API (New) and
 * updates the cached values in the database.
 *
 * Production notes:
 * - Never calls Google API from frontend requests
 * - Handles API errors gracefully — failures are logged but do not crash the app
 * - Each service is updated independently so one failure doesn't block others
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class GoogleRatingScheduler {

    private final ServiceRepository serviceRepository;
    private final GooglePlacesService googlePlacesService;

    /**
     * Sync Google ratings for all services with a configured googlePlaceId.
     * Runs daily at 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void syncGoogleRatings() {
        log.info("Starting scheduled Google Places rating sync...");

        List<Service> services = serviceRepository.findAllWithGooglePlaceId();

        if (services.isEmpty()) {
            log.info("No services with Google Place ID found. Skipping sync.");
            return;
        }

        log.info("Found {} services with Google Place ID to sync.", services.size());

        int successCount = 0;
        int failureCount = 0;

        for (Service service : services) {
            try {
                GoogleRatingResponse response = googlePlacesService.fetchRating(service.getGooglePlaceId());

                if (response != null) {
                    service.setGoogleRating(response.getRating());
                    service.setGoogleReviewCount(response.getUserRatingCount());
                    serviceRepository.save(service);
                    successCount++;
                    log.debug("Updated rating for service '{}' (id={}): rating={}, reviewCount={}",
                            service.getName(), service.getId(),
                            response.getRating(), response.getUserRatingCount());
                } else {
                    failureCount++;
                    log.warn("Failed to fetch rating for service '{}' (id={}, placeId={})",
                            service.getName(), service.getId(), service.getGooglePlaceId());
                }
            } catch (Exception e) {
                failureCount++;
                log.error("Error syncing rating for service '{}' (id={}, placeId={}): {}",
                        service.getName(), service.getId(), service.getGooglePlaceId(), e.getMessage());
            }
        }

        log.info("Google Places rating sync completed. Success: {}, Failures: {}, Total: {}",
                successCount, failureCount, services.size());
    }
}

