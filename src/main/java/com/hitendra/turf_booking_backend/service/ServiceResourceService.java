package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.service.CreateServiceResourceRequest;
import com.hitendra.turf_booking_backend.dto.service.ResourceDetailDto;
import com.hitendra.turf_booking_backend.dto.service.ResourcePriceRuleDto;
import com.hitendra.turf_booking_backend.dto.service.ResourceSlotConfigDto;
import com.hitendra.turf_booking_backend.dto.service.ServiceResourceDto;
import com.hitendra.turf_booking_backend.dto.service.UpdateServiceResourceRequest;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class ServiceResourceService {

    private final ServiceResourceRepository serviceResourceRepository;
    private final ServiceRepository serviceRepository;
    private final ResourceSlotConfigRepository resourceSlotConfigRepository;
    private final ActivityRepository activityRepository;
    private final ResourcePriceRuleRepository priceRuleRepository;

    /**
     * Get all resources for a service
     */
    @Transactional(readOnly = true)
    public List<ServiceResourceDto> getResourcesByServiceId(Long serviceId) {
        return serviceResourceRepository.findByServiceId(serviceId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all enabled resources for a service
     */
    @Transactional(readOnly = true)
    public List<ServiceResourceDto> getEnabledResourcesByServiceId(Long serviceId) {
        return serviceResourceRepository.findByServiceIdAndEnabledTrue(serviceId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get a resource by ID
     */
    @Transactional(readOnly = true)
    public ServiceResourceDto getResourceById(Long resourceId) {
        ServiceResource resource = serviceResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found with id: " + resourceId));
        return convertToDto(resource);
    }

    /**
     * Create a new resource with default slot configuration and activities
     *
     * FLOW:
     * 1. Create the ServiceResource
     * 2. Create default ResourceSlotConfig with provided slot details
     * 3. Associate activities with the resource
     */
    @Transactional
    public ServiceResourceDto createResource(CreateServiceResourceRequest request) {
        // STEP 1: Validate service exists
        Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + request.getServiceId()));

        // STEP 2: Check if resource with same name already exists for this service
        if (serviceResourceRepository.existsByServiceIdAndName(request.getServiceId(), request.getName())) {
            throw new RuntimeException("A resource with this name already exists for this service");
        }

        // STEP 3: Create the ServiceResource
        ServiceResource resource = ServiceResource.builder()
                .service(service)
                .name(request.getName())
                .description(request.getDescription())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .pricingType(request.getPricingType() != null
                        ? request.getPricingType()
                        : com.hitendra.turf_booking_backend.entity.PricingType.PER_SLOT)
                .maxPersonAllowed(request.getMaxPersonAllowed())
                .activities(new ArrayList<>())
                .build();

        ServiceResource savedResource = serviceResourceRepository.save(resource);
        log.info("Created resource '{}' (ID: {}) for service {}",
                savedResource.getName(), savedResource.getId(), service.getId());

        // STEP 4: Create default ResourceSlotConfig for this resource
        ResourceSlotConfig slotConfig = ResourceSlotConfig.builder()
                .resource(savedResource)
                .openingTime(request.getOpeningTime())
                .closingTime(request.getClosingTime())
                .slotDurationMinutes(request.getSlotDurationMinutes())
                .basePrice(request.getBasePrice())
                .enabled(true)
                .build();

        resourceSlotConfigRepository.save(slotConfig);
        log.info("Created default slot config for resource {} - Opening: {}, Closing: {}, Duration: {} mins, Base Price: {}",
                savedResource.getId(), request.getOpeningTime(), request.getClosingTime(),
                request.getSlotDurationMinutes(), request.getBasePrice());

        // STEP 5: Associate activities if provided
        if (request.getActivityCodes() != null && !request.getActivityCodes().isEmpty()) {
            List<Activity> activities = new ArrayList<>();

            for (String activityCode : request.getActivityCodes()) {
                Activity activity = activityRepository.findByCode(activityCode)
                        .orElseThrow(() -> new RuntimeException("Activity not found: " + activityCode));
                activities.add(activity);
            }

            // Add all activities to the resource
            savedResource.getActivities().addAll(activities);
            savedResource = serviceResourceRepository.save(savedResource);

            log.info("Associated {} activities to resource {} - Activities: {}",
                    activities.size(), savedResource.getId(), request.getActivityCodes());
        }

        return convertToDto(savedResource);
    }

    /**
     * Update a resource
     */
    @Transactional
    public ServiceResourceDto updateResource(Long resourceId, UpdateServiceResourceRequest request) {
        ServiceResource resource = serviceResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found with id: " + resourceId));

        if (request.getName() != null) {
            // Check if another resource with same name exists
            if (!resource.getName().equals(request.getName()) &&
                    serviceResourceRepository.existsByServiceIdAndName(resource.getService().getId(), request.getName())) {
                throw new RuntimeException("A resource with this name already exists for this service");
            }
            resource.setName(request.getName());
        }

        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription());
        }

        if (request.getEnabled() != null) {
            resource.setEnabled(request.getEnabled());
        }

        if (request.getPricingType() != null) {
            resource.setPricingType(request.getPricingType());
        }

        if (request.getMaxPersonAllowed() != null) {
            resource.setMaxPersonAllowed(request.getMaxPersonAllowed());
        }

        ServiceResource saved = serviceResourceRepository.save(resource);
        log.info("Updated resource {}", saved.getId());
        return convertToDto(saved);
    }

    /**
     * Delete a resource
     */
    @Transactional
    public void deleteResource(Long resourceId) {
        ServiceResource resource = serviceResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found with id: " + resourceId));

        serviceResourceRepository.delete(resource);
        log.info("Deleted resource {}", resourceId);
    }

    /**
     * Enable a resource
     */
    @Transactional
    public ServiceResourceDto enableResource(Long resourceId) {
        ServiceResource resource = serviceResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found with id: " + resourceId));

        resource.setEnabled(true);
        ServiceResource saved = serviceResourceRepository.save(resource);
        log.info("Enabled resource {}", resourceId);
        return convertToDto(saved);
    }

    /**
     * Disable a resource
     */
    @Transactional
    public ServiceResourceDto disableResource(Long resourceId) {
        ServiceResource resource = serviceResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found with id: " + resourceId));

        resource.setEnabled(false);
        ServiceResource saved = serviceResourceRepository.save(resource);
        log.info("Disabled resource {}", resourceId);
        return convertToDto(saved);
    }

    private ServiceResourceDto convertToDto(ServiceResource resource) {
        return ServiceResourceDto.builder()
                .id(resource.getId())
                .serviceId(resource.getService().getId())
                .serviceName(resource.getService().getName())
                .name(resource.getName())
                .description(resource.getDescription())
                .enabled(resource.isEnabled())
                .activities(resource.getActivities())
                .pricingType(resource.getPricingType() != null ? resource.getPricingType().name() : "PER_SLOT")
                .maxPersonAllowed(resource.getMaxPersonAllowed())
                .build();
    }

    /**
     * Get the complete detail of a single resource — manager only.
     * Includes slot configuration and ALL price rules (enabled + disabled).
     */
    @Transactional(readOnly = true)
    public ResourceDetailDto getResourceDetail(Long resourceId) {
        ServiceResource resource = serviceResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Resource not found with id: " + resourceId));
        return convertToDetailDto(resource);
    }

    /**
     * Build a ResourceDetailDto for a resource entity.
     * Used both by getResourceDetail and when embedding inside ServiceDetailDto.
     */
    public ResourceDetailDto convertToDetailDto(ServiceResource resource) {
        // Slot config (may be null if not yet configured)
        ResourceSlotConfigDto slotConfigDto = null;
        if (resource.getSlotConfig() != null) {
            ResourceSlotConfig cfg = resource.getSlotConfig();
            int totalSlots = cfg.getSlotDurationMinutes() > 0
                    ? (int) (java.time.Duration.between(cfg.getOpeningTime(), cfg.getClosingTime()).toMinutes()
                             / cfg.getSlotDurationMinutes())
                    : 0;
            slotConfigDto = ResourceSlotConfigDto.builder()
                    .id(cfg.getId())
                    .resourceId(resource.getId())
                    .resourceName(resource.getName())
                    .openingTime(cfg.getOpeningTime())
                    .closingTime(cfg.getClosingTime())
                    .slotDurationMinutes(cfg.getSlotDurationMinutes())
                    .basePrice(cfg.getBasePrice())
                    .enabled(cfg.isEnabled())
                    .totalSlots(totalSlots)
                    .build();
        }

        // All price rules (enabled + disabled), ordered by priority desc
        List<ResourcePriceRuleDto> priceRuleDtos = priceRuleRepository
                .findByResourceSlotConfig_Resource_IdOrderByPriorityDesc(resource.getId())
                .stream()
                .map(rule -> ResourcePriceRuleDto.builder()
                        .id(rule.getId())
                        .resourceId(resource.getId())
                        .resourceName(resource.getName())
                        .dayType(rule.getDayType())
                        .startTime(rule.getStartTime())
                        .endTime(rule.getEndTime())
                        .basePrice(rule.getBasePrice())
                        .extraCharge(rule.getExtraCharge())
                        .reason(rule.getReason())
                        .priority(rule.getPriority())
                        .enabled(rule.isEnabled())
                        .build())
                .collect(Collectors.toList());

        return ResourceDetailDto.builder()
                .id(resource.getId())
                .serviceId(resource.getService().getId())
                .serviceName(resource.getService().getName())
                .name(resource.getName())
                .description(resource.getDescription())
                .enabled(resource.isEnabled())
                .pricingType(resource.getPricingType() != null ? resource.getPricingType().name() : "PER_SLOT")
                .maxPersonAllowed(resource.getMaxPersonAllowed())
                .activities(resource.getActivities())
                .slotConfig(slotConfigDto)
                .priceRules(priceRuleDtos)
                .build();
    }
}

