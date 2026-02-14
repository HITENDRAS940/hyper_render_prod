package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.entity.InvoiceTemplate;
import com.hitendra.turf_booking_backend.exception.InvoiceException;
import com.hitendra.turf_booking_backend.repository.InvoiceTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for managing invoice templates with caching.
 *
 * CACHING STRATEGY:
 * - Active template is cached in memory
 * - Cache key: "activeInvoiceTemplate"
 * - Cache is evicted when manager updates template
 * - Prevents DB query on every invoice generation
 *
 * RULES:
 * - Only ONE template can be active at a time
 * - Old templates are retained for history
 * - Version is auto-incremented
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceTemplateService {

    private final InvoiceTemplateRepository templateRepository;

    /**
     * Get the currently active template.
     * Result is cached to avoid DB query on every invoice generation.
     *
     * Cache is evicted when manager updates template.
     */
    @Cacheable(value = "activeInvoiceTemplate")
    @Transactional(readOnly = true)
    public InvoiceTemplate getActiveTemplate() {
        log.info("Fetching active invoice template from database (cache miss)");
        return templateRepository.findByIsActiveTrue()
                .orElseThrow(() -> new InvoiceException("No active invoice template found. Please set one via admin API."));
    }

    /**
     * Create or update invoice template.
     *
     * FLOW:
     * 1. Deactivate all existing templates
     * 2. Create new template with incremented version
     * 3. Mark new template as active
     * 4. Evict cache
     *
     * @param name Template name
     * @param content HTML template content
     * @param createdBy Manager who created/updated the template
     * @return Newly created template
     */
    @Transactional
    @CacheEvict(value = "activeInvoiceTemplate", allEntries = true)
    public InvoiceTemplate createOrUpdateTemplate(String name, String content, String createdBy) {
        log.info("Creating new invoice template: {}", name);

        // Deactivate all existing templates
        templateRepository.deactivateAllTemplates();
        log.info("Deactivated all existing templates");

        // Get next version number
        Integer maxVersion = templateRepository.findMaxVersion();
        Integer newVersion = (maxVersion == null) ? 1 : maxVersion + 1;

        // Create new template
        InvoiceTemplate newTemplate = InvoiceTemplate.builder()
                .name(name)
                .content(content)
                .version(newVersion)
                .isActive(true)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        InvoiceTemplate saved = templateRepository.save(newTemplate);
        log.info("Created new template: ID={}, Version={}, Active={}", saved.getId(), saved.getVersion(), saved.getIsActive());

        // Cache is automatically evicted by @CacheEvict annotation
        log.info("Cache evicted. Next invoice generation will use new template.");

        return saved;
    }

    /**
     * Get template by version number (for rollback or history).
     */
    @Transactional(readOnly = true)
    public InvoiceTemplate getTemplateByVersion(Integer version) {
        return templateRepository.findByVersion(version)
                .orElseThrow(() -> new InvoiceException("Template version " + version + " not found"));
    }

    /**
     * Activate a specific template version (rollback scenario).
     * Deactivates current template and activates the specified one.
     */
    @Transactional
    @CacheEvict(value = "activeInvoiceTemplate", allEntries = true)
    public InvoiceTemplate activateTemplateVersion(Integer version, String updatedBy) {
        log.info("Activating template version: {}", version);

        InvoiceTemplate template = getTemplateByVersion(version);

        // Deactivate all templates
        templateRepository.deactivateAllTemplates();

        // Activate the specified template
        template.setIsActive(true);
        template.setUpdatedAt(LocalDateTime.now());
        template.setCreatedBy(updatedBy); // Update the person who activated it

        InvoiceTemplate saved = templateRepository.save(template);
        log.info("Activated template version: {}", version);

        return saved;
    }

    /**
     * Check if any active template exists.
     */
    @Transactional(readOnly = true)
    public boolean hasActiveTemplate() {
        return templateRepository.findByIsActiveTrue().isPresent();
    }
}


