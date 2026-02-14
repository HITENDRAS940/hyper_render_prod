package com.hitendra.turf_booking_backend.config;

import com.hitendra.turf_booking_backend.service.InvoiceTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Initialize default invoice template from file on first startup.
 * Runs after DataInitializer to ensure database tables exist.
 *
 * BEHAVIOR:
 * - Checks if any active template exists
 * - If not, loads default template from resources/templates/invoice-template.html
 * - Creates initial template with version 1
 * - This ensures invoice generation works immediately after deployment
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after DataInitializer (which is Order(1) by default)
public class InvoiceTemplateInitializer implements CommandLineRunner {

    private final InvoiceTemplateService templateService;

    @Override
    public void run(String... args) throws Exception {
        // Check if active template exists
        if (templateService.hasActiveTemplate()) {
            log.info("✅ Active invoice template already exists. Skipping initialization.");
            return;
        }

        log.info("🔧 No active invoice template found. Initializing default template...");

        try {
            // Load default template from resources/templates/invoice-template.html
            ClassPathResource resource = new ClassPathResource("templates/invoice-template.html");
            Path templatePath = resource.getFile().toPath();
            String templateContent = Files.readString(templatePath);

            // Create initial template
            templateService.createOrUpdateTemplate(
                    "Default Tax Invoice Template",
                    templateContent,
                    "system"
            );

            log.info("✅ Default invoice template initialized successfully (Version 1)");

        } catch (Exception e) {
            log.error("❌ Failed to initialize default invoice template. " +
                            "Invoice generation will fail until manager uploads a template via API.",
                    e);
            // Don't throw exception - allow app to start
            // Manager can upload template manually via API
        }
    }
}

