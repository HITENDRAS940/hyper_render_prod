package com.hitendra.turf_booking_backend.dto.invoice;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating or updating invoice template.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceTemplateRequest {

    /**
     * Template name for identification.
     */
    @NotBlank(message = "Template name is required")
    private String name;

    /**
     * HTML template content with Thymeleaf placeholders.
     */
    @NotBlank(message = "Template content is required")
    private String content;
}

