package com.hitendra.turf_booking_backend.dto.invoice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for invoice response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponseDto {
    private Long invoiceId;
    private Long bookingId;
    private String invoiceNumber;
    private String cloudinaryUrl;
    private Double invoiceAmount;
    private LocalDateTime createdAt;
}

