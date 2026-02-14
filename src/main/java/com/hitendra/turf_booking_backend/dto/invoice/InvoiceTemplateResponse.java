package com.hitendra.turf_booking_backend.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for invoice template response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceTemplateResponse {

    private Long id;
    private String name;
    private String content;
    private Integer version;
    private Boolean isActive;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

