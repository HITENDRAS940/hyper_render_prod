package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.common.PaginatedResponse;
import com.hitendra.turf_booking_backend.dto.service.*;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.repository.*;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.hitendra.turf_booking_backend.entity.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final CloudinaryService cloudinaryService;
    private final BookingRepository bookingRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final AuthUtil authUtil;
    private final LocationService locationService;
    private final ServiceResourceRepository serviceResourceRepository;
    private final ResourceSlotConfigRepository resourceSlotConfigRepository;
    private final ActivityRepository activityRepository;
    private final GooglePlacesService googlePlacesService;
    private final ServiceResourceService serviceResourceService;

    /**
     * Get all services without pagination.
     * 
     * @deprecated Use {@link #getAllServicesCard(int, int)} instead for better performance with large datasets.
     * This method loads all services into memory which can cause performance issues.
     */
    @Deprecated
    public List<ServiceDto> getAllServices() {
        log.warn("PERFORMANCE WARNING: getAllServices() called without pagination. " +
                 "Consider using getAllServicesCard(page, size) for better performance.");
        return serviceRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public PaginatedResponse<ServiceCardDto> getAllServicesCard(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Step 1: Get paginated service IDs (no join with images table)
        Page<Long> serviceIdsPage = serviceRepository.findAllServiceIds(pageable);

        // Step 2: Fetch full service entities by IDs (with images)
        List<com.hitendra.turf_booking_backend.entity.Service> services =
            serviceIdsPage.getContent().isEmpty() ?
            List.of() :
            serviceRepository.findServicesByIds(serviceIdsPage.getContent());

        // Convert to DTOs
        List<ServiceCardDto> content = services.stream()
                .map(this::convertToCardDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                serviceIdsPage.getNumber(),
                serviceIdsPage.getSize(),
                serviceIdsPage.getTotalElements(),
                serviceIdsPage.getTotalPages(),
                serviceIdsPage.isLast()
        );
    }

    public PaginatedResponse<ServiceDto> getServicesByAdminId(Long adminProfileId, int page, int size) {
        // Verify admin profile exists (optimized)
        if (!adminProfileRepository.existsById(adminProfileId)) {
            throw new RuntimeException("Admin profile not found with id: " + adminProfileId);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<com.hitendra.turf_booking_backend.entity.Service> servicePage = serviceRepository.findByCreatedById(adminProfileId, pageable);

        List<ServiceDto> content = servicePage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                servicePage.getNumber(),
                servicePage.getSize(),
                servicePage.getTotalElements(),
                servicePage.getTotalPages(),
                servicePage.isLast()
        );
    }

    /**
     * Get a lightweight list of services for admin dashboard.
     * Only fetches id, name, location, city, and availability for optimization.
     */
    public PaginatedResponse<AdminServiceSummaryDto> getAdminServiceSummaryByUserId(Long userId, int page, int size) {
        // Find admin profile by user ID
        AdminProfile adminProfile = adminProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Admin profile not found for user id: " + userId));

        Pageable pageable = PageRequest.of(page, size);
        Page<AdminServiceSummaryDto> servicePage = serviceRepository.findAdminServiceSummaryByCreatedById(adminProfile.getId(), pageable);

        return new PaginatedResponse<>(
                servicePage.getContent(),
                servicePage.getNumber(),
                servicePage.getSize(),
                servicePage.getTotalElements(),
                servicePage.getTotalPages(),
                servicePage.isLast()
        );
    }

    public ServiceDto getServiceById(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        return convertToDto(service);
    }

    /**
     * Get the complete detail of a service — manager only.
     * Includes every service field plus each resource's slot config and all price rules.
     */
    @Transactional(readOnly = true)
    public ServiceDetailDto getServiceDetail(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));

        // Build resource detail list
        List<ResourceDetailDto> resourceDetails = service.getResources().stream()
                .map(serviceResourceService::convertToDetailDto)
                .collect(Collectors.toList());

        // Resolve admin info
        Long createdByAdminId = null;
        String createdByAdminName = null;
        if (service.getCreatedBy() != null) {
            createdByAdminId = service.getCreatedBy().getId();
            if (service.getCreatedBy().getUser() != null) {
                createdByAdminName = service.getCreatedBy().getUser().getName();
            }
        }

        return ServiceDetailDto.builder()
                .id(service.getId())
                .name(service.getName())
                .location(service.getLocation())
                .city(service.getCity())
                .state(service.getState())
                .latitude(service.getLatitude())
                .longitude(service.getLongitude())
                .description(service.getDescription())
                .contactNumber(service.getContactNumber())
                .gstin(service.getGstin())
                .startTime(service.getStartTime())
                .endTime(service.getEndTime())
                .availability(service.isAvailability())
                .refundAllowed(service.isRefundAllowed())
                .amenities(service.getAmenities())
                .images(service.getImages())
                .activities(service.getActivities() != null
                        ? service.getActivities().stream()
                                .map(Activity::getName)
                                .collect(Collectors.toList())
                        : List.of())
                .googlePlaceId(service.getGooglePlaceId())
                .googleRating(service.getGoogleRating())
                .googleReviewCount(service.getGoogleReviewCount())
                .createdByAdminId(createdByAdminId)
                .createdByAdminName(createdByAdminName)
                .resources(resourceDetails)
                .build();
    }

    /**
     * Create service with basic details AND activities AND amenities
     * Associates specified activities with the service and sets amenities
     */
    public ServiceDto createServiceDetails(CreateServiceRequest request, Long adminProfileId) {
        // Get admin profile
        AdminProfile adminProfile = adminProfileRepository.findById(adminProfileId)
                .orElseThrow(() -> new RuntimeException("Admin profile not found"));

        // Create service
        Service service = com.hitendra.turf_booking_backend.entity.Service.builder()
                .name(request.getName())
                .location(request.getLocation())
                .city(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .description(request.getDescription())
                .contactNumber(request.getContactNumber())
                .createdBy(adminProfile)
                .refundAllowed(request.getRefundAllowed() != null ? request.getRefundAllowed() : true)
                .googlePlaceId(request.getGooglePlaceId())
                .images(new ArrayList<>())
                .activities(new ArrayList<>())  // Initialize activities list
                .amenities(new ArrayList<>())   // Initialize amenities list
                .build();

        // Add amenities if provided
        if (request.getAmenities() != null && !request.getAmenities().isEmpty()) {
            service.getAmenities().addAll(request.getAmenities());
        }

        // Save service first
        Service savedService = serviceRepository.save(service);

        // Auto-discover Google Place and fetch rating using service name + city
        try {
            String placeId = request.getGooglePlaceId();

            if (placeId != null && !placeId.isBlank()) {
                // If placeId was explicitly provided, fetch rating directly
                var ratingResponse = googlePlacesService.fetchRating(placeId);
                if (ratingResponse != null) {
                    savedService.setGoogleRating(ratingResponse.getRating());
                    savedService.setGoogleReviewCount(ratingResponse.getUserRatingCount());
                    savedService = serviceRepository.save(savedService);
                    log.info("Google rating fetched for new service '{}' (explicit placeId): rating={}, reviews={}",
                            savedService.getName(), ratingResponse.getRating(), ratingResponse.getUserRatingCount());
                }
            } else {
                // Search by name + city to auto-discover placeId and rating
                var searchResult = googlePlacesService.searchAndFetchRating(request.getName(), request.getCity());
                if (searchResult != null) {
                    savedService.setGooglePlaceId(searchResult.placeId());
                    savedService.setGoogleRating(searchResult.rating());
                    savedService.setGoogleReviewCount(searchResult.userRatingCount());
                    savedService = serviceRepository.save(savedService);
                    log.info("Google Place auto-discovered for new service '{}': placeId={}, rating={}, reviews={}",
                            savedService.getName(), searchResult.placeId(),
                            searchResult.rating(), searchResult.userRatingCount());
                } else {
                    log.info("No Google Place found for service '{}' in city '{}'",
                            request.getName(), request.getCity());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch Google rating for new service '{}': {}",
                    savedService.getName(), e.getMessage());
            // Don't fail service creation if rating fetch fails
        }

        // Associate activities if provided
        if (request.getActivityCodes() != null && !request.getActivityCodes().isEmpty()) {
            List<Activity> activities = new ArrayList<>();

            for (String activityCode : request.getActivityCodes()) {
                Activity activity = activityRepository.findByCode(activityCode)
                        .orElseThrow(() -> new RuntimeException("Activity not found: " + activityCode));
                activities.add(activity);
            }

            // Add all activities to the service
            savedService.getActivities().addAll(activities);

            // Save service with activities
            savedService = serviceRepository.save(savedService);
        }

        return convertToDto(savedService);
    }

    /**
     * Check if current user can modify the service
     * Only the admin who created the service or a manager can modify it
     */
    private void checkServiceModificationPermission(com.hitendra.turf_booking_backend.entity.Service service) {
        User user = authUtil.getCurrentUser();
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // Managers can modify any service
        if (user.getRole() == Role.MANAGER) {
            return;
        }

        // Admins can only modify services they created
        if (user.getRole() == Role.ADMIN) {
            AdminProfile adminProfile = adminProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Admin profile not found"));

            if (service.getCreatedBy() == null || !service.getCreatedBy().getId().equals(adminProfile.getId())) {
                throw new RuntimeException("You can only modify services that you created");
            }
        } else {
            throw new RuntimeException("You don't have permission to modify this service");
        }
    }

    public ServiceDto updateService(Long id, CreateServiceRequest serviceDto) {
        com.hitendra.turf_booking_backend.entity.Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        // Check if current user has permission to modify this service
        checkServiceModificationPermission(service);

        service.setName(serviceDto.getName());
        service.setLocation(serviceDto.getLocation());
        service.setCity(serviceDto.getCity());
        service.setLatitude(serviceDto.getLatitude());
        service.setLongitude(serviceDto.getLongitude());
        service.setDescription(serviceDto.getDescription());
        service.setContactNumber(serviceDto.getContactNumber());
        if (serviceDto.getRefundAllowed() != null) {
            service.setRefundAllowed(serviceDto.getRefundAllowed());
        }

        // Update Google Place ID and fetch initial rating if changed
        if (serviceDto.getGooglePlaceId() != null) {
            String oldPlaceId = service.getGooglePlaceId();
            service.setGooglePlaceId(serviceDto.getGooglePlaceId());

            // Fetch rating immediately if placeId is new or changed
            if (!serviceDto.getGooglePlaceId().isBlank()
                    && !serviceDto.getGooglePlaceId().equals(oldPlaceId)) {
                try {
                    var ratingResponse = googlePlacesService.fetchRating(serviceDto.getGooglePlaceId());
                    if (ratingResponse != null) {
                        service.setGoogleRating(ratingResponse.getRating());
                        service.setGoogleReviewCount(ratingResponse.getUserRatingCount());
                        log.info("Google rating updated for service '{}': rating={}, reviews={}",
                                service.getName(), ratingResponse.getRating(), ratingResponse.getUserRatingCount());
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch Google rating for service '{}': {}",
                            service.getName(), e.getMessage());
                }
            } else if (serviceDto.getGooglePlaceId().isBlank()) {
                // Clear rating if placeId is cleared
                service.setGoogleRating(null);
                service.setGoogleReviewCount(null);
            }
        }

        com.hitendra.turf_booking_backend.entity.Service updated = serviceRepository.save(service);
        return convertToDto(updated);
    }

    /**
     * Update service location from a Google Maps URL
     * Extracts latitude and longitude from the URL and saves to the service
     */
    public ServiceDto updateServiceLocationFromUrl(Long serviceId, String locationUrl) {
        com.hitendra.turf_booking_backend.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        // Extract coordinates from URL
        double[] coordinates = locationService.extractCoordinatesFromUrl(locationUrl);

        service.setLatitude(coordinates[0]);
        service.setLongitude(coordinates[1]);

        com.hitendra.turf_booking_backend.entity.Service updated = serviceRepository.save(service);
        return convertToDto(updated);
    }

    /**
     * Upload images for an existing service (maximum 4 images per upload)
     * Only the admin who created the service or a manager can upload images
     */
    public ServiceImageUploadResponse uploadServiceImages(Long serviceId, List<MultipartFile> images) {
        com.hitendra.turf_booking_backend.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + serviceId));

        // Check if current user has permission to modify this service
        checkServiceModificationPermission(service);

        if (images == null || images.isEmpty()) {
            throw new RuntimeException("No images provided for upload");
        }

        // Validate maximum 4 images per upload
        if (images.size() > 4) {
            throw new RuntimeException("Maximum 4 images can be uploaded at once. You provided " + images.size() + " images");
        }

        // Filter out empty files
        List<MultipartFile> validImages = images.stream()
                .filter(file -> file != null && !file.isEmpty())
                .collect(Collectors.toList());

        if (validImages.isEmpty()) {
            throw new RuntimeException("No valid images provided for upload");
        }

        if (validImages.size() > 4) {
            throw new RuntimeException("Maximum 4 images can be uploaded at once");
        }

        // Upload images to Cloudinary
        List<String> uploadedImageUrls = cloudinaryService.uploadImages(validImages);

        // Add new image URLs to existing images
        List<String> currentImages = service.getImages();
        if (currentImages == null) {
            currentImages = new ArrayList<>();
        }
        currentImages.addAll(uploadedImageUrls);
        service.setImages(currentImages);

        serviceRepository.save(service);

        return ServiceImageUploadResponse.builder()
                .serviceId(serviceId)
                .message("Images uploaded successfully")
                .uploadedImageUrls(uploadedImageUrls)
                .totalImages(currentImages.size())
                .build();
    }

    /**
     * Delete specific images from a service
     * Only the admin who created the service or a manager can delete images
     */
    public void deleteSpecificImages(Long serviceId, List<String> imageUrlsToDelete) {
        com.hitendra.turf_booking_backend.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        // Check if current user has permission to modify this service
        checkServiceModificationPermission(service);

        // Delete images from Cloudinary
        cloudinaryService.deleteImages(imageUrlsToDelete);

        // Remove URLs from service entity
        List<String> updatedImages = service.getImages().stream()
                .filter(url -> !imageUrlsToDelete.contains(url))
                .collect(Collectors.toList());

        service.setImages(updatedImages);
        serviceRepository.save(service);
    }

    /**
     * Delete a service and all its images from Cloudinary
     * Only the admin who created the service or a manager can delete it
     */
    @Transactional
    public void deleteService(Long id) {
        // First verify service exists
        com.hitendra.turf_booking_backend.entity.Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));

        // Check if current user has permission to modify this service
        checkServiceModificationPermission(service);

        // Check if there are any bookings for this service
        List<Booking> existingBookings = bookingRepository.findByServiceId(id);

        if (!existingBookings.isEmpty()) {
            // Count confirmed bookings
            long confirmedBookings = existingBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                    .count();

            // Count pending bookings
            long pendingBookings = existingBookings.stream()
                    .filter(b -> b.getStatus() == BookingStatus.PENDING)
                    .count();

            // Build detailed error message
            String errorMessage;
            if (confirmedBookings > 0) {
                errorMessage = String.format(
                    "Cannot delete service '%s'. There are %d confirmed booking(s) associated with this service. " +
                    "Bookings must be completed or cancelled before deletion.",
                    service.getName(), confirmedBookings
                );
            } else if (pendingBookings > 0) {
                errorMessage = String.format(
                    "Cannot delete service '%s'. There are %d pending booking(s) being processed. " +
                    "Please wait for bookings to complete or cancel them before deletion.",
                    service.getName(), pendingBookings
                );
            } else {
                // Cancelled/completed bookings exist
                errorMessage = String.format(
                    "Cannot delete service '%s'. There are %d historical booking record(s) associated with this service. " +
                    "Booking history must be preserved for audit purposes.",
                    service.getName(), existingBookings.size()
                );
            }

            throw new RuntimeException(errorMessage);
        }

        // Delete all images from Cloudinary before deleting the service
        if (service.getImages() != null && !service.getImages().isEmpty()) {
            try {
                cloudinaryService.deleteImages(service.getImages());
            } catch (Exception e) {
                // Log error but continue with deletion
                System.err.println("Warning: Failed to delete some images from Cloudinary: " + e.getMessage());
            }
        }

        // Delete the service
        serviceRepository.deleteById(id);
    }

    /**
     * Force-delete a service and ALL its associated data (manager only).
     *
     * What gets deleted:
     *  - All service images (from Cloudinary)
     *  - All resources (cascade CascadeType.ALL on Service.resources)
     *  - All slot configs (cascade on ServiceResource.slotConfig)
     *  - All price rules (cascade via ServiceResource)
     *  - Bookings are preserved but their service_id is set to NULL (ON DELETE SET NULL in DB)
     *
     * No booking-count check — this is a hard delete for manager use only.
     */
    @Transactional
    public void forceDeleteService(Long id) {
        com.hitendra.turf_booking_backend.entity.Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found: " + id));

        log.info("FORCE DELETE initiated for service {} '{}' by manager", id, service.getName());

        // 1. Delete all Cloudinary images
        if (service.getImages() != null && !service.getImages().isEmpty()) {
            try {
                cloudinaryService.deleteImages(service.getImages());
                log.info("Deleted {} Cloudinary images for service {}", service.getImages().size(), id);
            } catch (Exception e) {
                log.warn("Failed to delete some Cloudinary images for service {}: {}", id, e.getMessage());
            }
        }

        // 2. Delete service — cascade handles resources, slot configs, price rules
        serviceRepository.delete(service);

        log.info("FORCE DELETE completed for service {} — resources, slot configs removed; bookings preserved with service_id=NULL", id);
    }

    private ServiceDto convertToDto(Service service) {
        ServiceDto dto = new ServiceDto();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setLocation(service.getLocation());
        dto.setCity(service.getCity());
        dto.setLatitude(service.getLatitude());
        dto.setLongitude(service.getLongitude());
        dto.setDescription(service.getDescription());
        dto.setContactNumber(service.getContactNumber());
        dto.setImages(service.getImages());
        dto.setAvailability(service.isAvailability());
        dto.setRefundAllowed(service.isRefundAllowed());
        dto.setAmenities(service.getAmenities());
        dto.setGoogleRating(service.getGoogleRating());
        dto.setGoogleReviewCount(service.getGoogleReviewCount());

        if (service.getActivities() != null) {
            dto.setActivities(service.getActivities().stream()
                    .map(Activity::getName)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private ServiceCardDto convertToCardDto(Service service) {
        ServiceCardDto dto = new ServiceCardDto();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setLocation(service.getLocation());
        dto.setAvailability(service.isAvailability());
        dto.setImages(service.getImages());
        dto.setDescription(service.getDescription());
        dto.setGoogleRating(service.getGoogleRating());
        dto.setGoogleReviewCount(service.getGoogleReviewCount());
        return dto;
    }

    public void serviceAvailable(Long serviceId) {
        com.hitendra.turf_booking_backend.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + serviceId));
        service.setAvailability(true);
        serviceRepository.save(service);
    }

    public void serviceNotAvailable(Long serviceId) {
        com.hitendra.turf_booking_backend.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + serviceId));
        service.setAvailability(false);
        serviceRepository.save(service);
    }

    /**
     * Get service availability status
     */
    public boolean getServiceAvailability(Long serviceId) {
        com.hitendra.turf_booking_backend.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + serviceId));
        return service.isAvailability();
    }

    /**
     * Check if service is available for booking
     */
    public Boolean isServiceAvailable(Long serviceId) {
        return getServiceAvailability(serviceId);
    }

    /**
     * Get the lowest price among all resource slot configs for a specific service
     */
    public Double getLowestSlotPrice(Long serviceId) {
        // Verify service exists
        serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + serviceId));

        List<ServiceResource> resources = serviceResourceRepository.findByServiceId(serviceId);

        return resources.stream()
                .filter(ServiceResource::isEnabled)
                .map(r -> resourceSlotConfigRepository.findByResourceId(r.getId()).orElse(null))
                .filter(config -> config != null && config.isEnabled())
                .mapToDouble(ResourceSlotConfig::getBasePrice)
                .min()
                .orElseThrow(() -> new RuntimeException("No slot configs found for service with id: " + serviceId));
    }

    /**
     * Get services by city name
     * OPTIMIZED: Uses repository query instead of loading all services
     */
    public List<ServiceDto> getServicesByCity(String city) {
        return serviceRepository.findByCityIgnoreCase(city).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get services by city name with pagination
     */
    public PaginatedResponse<ServiceDto> getServicesByCity(String city, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<com.hitendra.turf_booking_backend.entity.Service> servicePage = serviceRepository.findByCityIgnoreCase(city, pageable);

        List<ServiceDto> content = servicePage.getContent().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                servicePage.getNumber(),
                servicePage.getSize(),
                servicePage.getTotalElements(),
                servicePage.getTotalPages(),
                servicePage.isLast()
        );
    }

    /**
     * Get services by city name with pagination (Card view)
     * OPTIMIZED: Only fetches required fields (id, name, location, availability, images, description)
     */
    public PaginatedResponse<ServiceCardDto> getServicesCardByCity(String city, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Step 1: Get paginated service IDs by city (no join with images table)
        Page<Long> serviceIdsPage = serviceRepository.findServiceIdsByCity(city, pageable);

        // Step 2: Fetch full service entities by IDs (with images)
        List<com.hitendra.turf_booking_backend.entity.Service> services =
            serviceIdsPage.getContent().isEmpty() ?
            List.of() :
            serviceRepository.findServicesByIds(serviceIdsPage.getContent());

        // Convert to DTOs
        List<ServiceCardDto> content = services.stream()
                .map(this::convertToCardDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                serviceIdsPage.getNumber(),
                serviceIdsPage.getSize(),
                serviceIdsPage.getTotalElements(),
                serviceIdsPage.getTotalPages(),
                serviceIdsPage.isLast()
        );
    }

    /**
     * Get services by city and activity code with pagination
     */
    public PaginatedResponse<ServiceCardDto> getServicesByCityAndActivity(String city, String activityCode, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<com.hitendra.turf_booking_backend.entity.Service> servicePage = serviceRepository.findByCityAndActivityCode(city, activityCode, pageable);

        List<ServiceCardDto> content = servicePage.getContent().stream()
                .map(this::convertToCardDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                servicePage.getNumber(),
                servicePage.getSize(),
                servicePage.getTotalElements(),
                servicePage.getTotalPages(),
                servicePage.isLast()
        );
    }

    /**
     * Get all unique cities where services are available
     * OPTIMIZED: Direct query for distinct cities without loading full entities
     */
    public List<String> getAvailableCities() {
        return serviceRepository.findAllDistinctCities();
    }

    /**
     * Search services by date, time range, and activity availability.
     *
     * OPTIMIZED APPROACH (Single SQL Query):
     * Uses a single database query to find services that match the criteria and do NOT have
     * any overlapping bookings or disabled slots.
     */
    public List<ServiceSearchDto> searchServicesByAvailability(LocalDate date, LocalTime startTime, LocalTime endTime, String city, String activityCode) {
        // Handle empty strings as null for the repository query
        String searchCity = (city != null && !city.trim().isEmpty()) ? city : null;
        String searchActivity = (activityCode != null && !activityCode.trim().isEmpty()) ? activityCode : null;

        // Determine effective time range
        ZoneId istZone = ZoneId.of("Asia/Kolkata");
        ZonedDateTime nowIST = ZonedDateTime.now(istZone);
        LocalDate todayIST = nowIST.toLocalDate();
        LocalTime timeIST = nowIST.toLocalTime();

        LocalTime effectiveStartTime = (startTime != null) ? startTime : LocalTime.MIN;
        LocalTime effectiveEndTime = (endTime != null) ? endTime : LocalTime.MAX;

        // If date is today, ensure we don't search in the past
        if (date.equals(todayIST)) {
            if (effectiveStartTime.isBefore(timeIST)) {
                effectiveStartTime = timeIST;
            }
        } else if (date.isBefore(todayIST)) {
            // Past dates are never available
            return new ArrayList<>();
        }

        // Execute single optimized query with DTO projection
        return serviceRepository.findAvailableServicesDto(
                date, effectiveStartTime, effectiveEndTime, searchCity, searchActivity
        );
    }

    /**
     * Search services by keyword, city, and activity
     */
    public List<ServiceSearchDto> searchServices(String keyword, String city, String activity) {

        // Handle empty strings as null for the repository query
        String searchCity = (city != null && !city.trim().isEmpty()) ? city : null;
        String searchActivity = (activity != null && !activity.trim().isEmpty()) ? activity : null;
        String searchKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword : null;

        List<Service> searchedServices = serviceRepository.searchServices(searchCity, searchActivity, searchKeyword);

        return searchedServices.stream()
                .map(this::convertToSearchDto)
                .collect(Collectors.toList());
    }

    private ServiceSearchDto convertToSearchDto(Service service) {
        ServiceSearchDto dto = new ServiceSearchDto();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setLocation(service.getLocation());
        return dto;
    }
}

