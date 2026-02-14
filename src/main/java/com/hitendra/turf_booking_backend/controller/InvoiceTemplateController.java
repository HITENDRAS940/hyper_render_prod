package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.invoice.InvoiceTemplateRequest;
import com.hitendra.turf_booking_backend.dto.invoice.InvoiceTemplateResponse;
import com.hitendra.turf_booking_backend.entity.InvoiceTemplate;
import com.hitendra.turf_booking_backend.service.InvoiceTemplateService;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Manager API for managing invoice templates.
 *
 * FEATURE FLAG:
 * - This controller is ONLY created if invoice.generation.enabled=true
 * - If disabled, template management endpoints will return 404
 * - Keeps application lightweight for free-tier deployments
 *
 * FEATURES:
 * - Create/update invoice template without redeploying backend
 * - View active template
 * - Activate specific template version (rollback)
 * - Cache is automatically evicted on template update
 *
 * SECURITY:
 * - Only MANAGER role can access these endpoints
 */
@RestController
@RequestMapping("/api/manager/invoice-template")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Manager - Invoice Template", description = "Manage invoice templates without backend redeploy")
@SecurityRequirement(name = "Bearer Authentication")
@ConditionalOnProperty(
    prefix = "invoice.generation",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class InvoiceTemplateController {

    private final InvoiceTemplateService templateService;
    private final AuthUtil authUtil;

    /**
     * Create or update invoice template.
     * Deactivates old template and activates new one.
     * Cache is automatically evicted.
     *
     * POST /api/manager/invoice-template
     */
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
        summary = "Create or Update Invoice Template",
        description = "Upload new HTML template. Previous template is deactivated. Cache is evicted. No backend redeploy needed."
    )
    public ResponseEntity<InvoiceTemplateResponse> createOrUpdateTemplate(
            @Valid @RequestBody InvoiceTemplateRequest request) {

        log.info("📝 Manager creating/updating invoice template: {}", request.getName());

        String managerEmail = authUtil.getCurrentUserEmail();

        InvoiceTemplate template = templateService.createOrUpdateTemplate(
                request.getName(),
                request.getContent(),
                managerEmail
        );

        InvoiceTemplateResponse response = mapToResponse(template);

        log.info("✅ Template created: Version={}, Active={}", template.getVersion(), template.getIsActive());

        return ResponseEntity.ok(response);
    }

    /**
     * Get currently active template.
     *
     * GET /api/manager/invoice-template/active
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
        summary = "Get Active Template",
        description = "Retrieve the currently active invoice template (cached in memory)"
    )
    public ResponseEntity<InvoiceTemplateResponse> getActiveTemplate() {
        log.info("Fetching active invoice template");

        InvoiceTemplate template = templateService.getActiveTemplate();
        InvoiceTemplateResponse response = mapToResponse(template);

        return ResponseEntity.ok(response);
    }

    /**
     * Activate specific template version (rollback scenario).
     *
     * PUT /api/manager/invoice-template/activate/{version}
     */
    @PutMapping("/activate/{version}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
        summary = "Activate Template Version",
        description = "Rollback to a previous template version. Deactivates current template and activates specified version."
    )
    public ResponseEntity<InvoiceTemplateResponse> activateTemplateVersion(
            @PathVariable Integer version) {

        log.info("🔄 Manager activating template version: {}", version);

        String managerEmail = authUtil.getCurrentUserEmail();

        InvoiceTemplate template = templateService.activateTemplateVersion(version, managerEmail);
        InvoiceTemplateResponse response = mapToResponse(template);

        log.info("✅ Template version {} activated", version);

        return ResponseEntity.ok(response);
    }

    /**
     * Get template by version.
     *
     * GET /api/manager/invoice-template/{version}
     */
    @GetMapping("/{version}")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
        summary = "Get Template by Version",
        description = "Retrieve a specific template version for review"
    )
    public ResponseEntity<InvoiceTemplateResponse> getTemplateByVersion(
            @PathVariable Integer version) {

        log.info("Fetching template version: {}", version);

        InvoiceTemplate template = templateService.getTemplateByVersion(version);
        InvoiceTemplateResponse response = mapToResponse(template);

        return ResponseEntity.ok(response);
    }

    /**
     * Check if active template exists (health check).
     *
     * GET /api/manager/invoice-template/health
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
        summary = "Template Health Check",
        description = "Check if an active template exists in the system"
    )
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean hasActiveTemplate = templateService.hasActiveTemplate();

        return ResponseEntity.ok(Map.of(
                "hasActiveTemplate", hasActiveTemplate,
                "status", hasActiveTemplate ? "OK" : "WARNING",
                "message", hasActiveTemplate
                        ? "Active template exists"
                        : "No active template found. Please create one."
        ));
    }

    /**
     * Map entity to response DTO.
     */
    private InvoiceTemplateResponse mapToResponse(InvoiceTemplate template) {
        return InvoiceTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .content(template.getContent())
                .version(template.getVersion())
                .isActive(template.getIsActive())
                .createdBy(template.getCreatedBy())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}



