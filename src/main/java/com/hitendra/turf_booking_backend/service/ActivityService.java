package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.activity.CreateActivityDto;
import com.hitendra.turf_booking_backend.dto.activity.GetActivityDto;
import com.hitendra.turf_booking_backend.entity.Activity;
import com.hitendra.turf_booking_backend.repository.ActivityRepository;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;

    /**
     * Get all activities with caching.
     * OPTIMIZED: Activities are cached since they rarely change.
     */
    @Cacheable(value = "activities", unless = "#result == null || #result.isEmpty()")
    public List<GetActivityDto> getAllActivities() {
        return activityRepository.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Create a new activity (Manager only).
     */
    @CacheEvict(value = "activities", allEntries = true)
    public GetActivityDto createActivity(CreateActivityDto request) {
        String code = request.getCode().trim().toUpperCase();
        if (activityRepository.existsByCode(code)) {
            throw new RuntimeException("Activity with code '" + code + "' already exists");
        }
        Activity activity = Activity.builder()
                .code(code)
                .name(request.getName().trim())
                .enabled(true)
                .build();
        return convertToDto(activityRepository.save(activity));
    }

    /**
     * Update an existing activity (Manager only).
     */
    @CacheEvict(value = "activities", allEntries = true)
    public GetActivityDto updateActivity(Long id, CreateActivityDto request) {
        Activity activity = findById(id);
        String newCode = request.getCode().trim().toUpperCase();
        // Allow same code on the same entity, but reject if another entity owns it
        activityRepository.findByCode(newCode).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("Activity with code '" + newCode + "' already exists");
            }
        });
        activity.setCode(newCode);
        activity.setName(request.getName().trim());
        return convertToDto(activityRepository.save(activity));
    }

    /**
     * Delete an activity permanently (Manager only).
     */
    @CacheEvict(value = "activities", allEntries = true)
    public void deleteActivity(Long id) {
        Activity activity = findById(id);
        activityRepository.delete(activity);
    }

    /**
     * Enable an activity (Manager only).
     */
    @CacheEvict(value = "activities", allEntries = true)
    public GetActivityDto enableActivity(Long id) {
        Activity activity = findById(id);
        activity.setEnabled(true);
        return convertToDto(activityRepository.save(activity));
    }

    /**
     * Disable an activity (Manager only).
     */
    @CacheEvict(value = "activities", allEntries = true)
    public GetActivityDto disableActivity(Long id) {
        Activity activity = findById(id);
        activity.setEnabled(false);
        return convertToDto(activityRepository.save(activity));
    }

    public Activity getActivityById(Long id) {
        return findById(id);
    }

    // ==================== Helpers ====================

    private Activity findById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found with id: " + id));
    }

    private GetActivityDto convertToDto(Activity activity) {
        GetActivityDto dto = new GetActivityDto();
        dto.setId(activity.getId());
        dto.setCode(activity.getCode());
        dto.setName(activity.getName());
        dto.setEnabled(activity.isEnabled());
        return dto;
    }
}
