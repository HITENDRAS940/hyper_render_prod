package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.InvoiceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for InvoiceTemplate entity.
 */
@Repository
public interface InvoiceTemplateRepository extends JpaRepository<InvoiceTemplate, Long> {

    /**
     * Find the currently active template.
     * Only one template should have isActive = true.
     */
    Optional<InvoiceTemplate> findByIsActiveTrue();

    /**
     * Find template by version number.
     */
    Optional<InvoiceTemplate> findByVersion(Integer version);

    /**
     * Get the maximum version number (for auto-incrementing).
     */
    @Query("SELECT MAX(t.version) FROM InvoiceTemplate t")
    Integer findMaxVersion();

    /**
     * Deactivate all templates (used before activating a new one).
     * Ensures only one template is active at a time.
     */
    @Modifying
    @Query("UPDATE InvoiceTemplate t SET t.isActive = false WHERE t.isActive = true")
    void deactivateAllTemplates();
}

