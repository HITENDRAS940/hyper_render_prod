package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Invoice Template entity for storing HTML templates in database.
 *
 * RULES:
 * - Only ONE template can have isActive = true at any time
 * - Manager can update template content without redeploying backend
 * - Old templates are retained for history and rollback
 * - Each template has a version number for tracking
 */
@Entity
@Table(name = "invoice_templates", indexes = {
    @Index(name = "idx_invoice_template_active", columnList = "is_active"),
    @Index(name = "idx_invoice_template_version", columnList = "version")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Template name for identification (e.g., "Tax Invoice V1", "Standard Invoice").
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * HTML template content (stored as TEXT).
     * This is the Thymeleaf template with placeholders.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Version number for tracking template changes.
     * Auto-incremented when new template is created.
     */
    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * Only one template should have isActive = true.
     * This is the template used for generating new invoices.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    /**
     * When the template was created.
     */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Last update timestamp (when template was modified or activated).
     */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Who created/updated this template (manager email or username).
     */
    @Column(name = "created_by", length = 255)
    private String createdBy;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

