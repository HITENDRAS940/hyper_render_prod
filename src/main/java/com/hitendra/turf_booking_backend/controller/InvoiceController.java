package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.invoice.InvoiceResponseDto;
import com.hitendra.turf_booking_backend.entity.Invoice;
import com.hitendra.turf_booking_backend.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for invoice management.
 *
 * Provides API to fetch invoice URLs for confirmed bookings.
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Invoice Management", description = "APIs for fetching booking invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Get invoice by booking ID.
     *
     * Returns the Cloudinary secure URL for the invoice PDF.
     *
     * @param bookingId Booking ID
     * @return InvoiceResponseDto with invoice details
     */
    @GetMapping("/{bookingId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'MANAGER')")
    @Operation(
        summary = "Get invoice by booking ID",
        description = """
            Retrieves the invoice for a confirmed booking.
            
            **Response:**
            - Invoice number (INV-{bookingId})
            - Cloudinary secure URL for PDF download
            - Invoice amount and creation timestamp
            
            **Error Cases:**
            - 404: Invoice not found (booking not confirmed or invoice generation failed)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    public ResponseEntity<InvoiceResponseDto> getInvoiceByBookingId(
            @Parameter(description = "Booking ID", required = true)
            @PathVariable Long bookingId) {

        log.info("Fetching invoice for booking ID: {}", bookingId);

        Invoice invoice = invoiceService.getInvoiceByBookingId(bookingId);

        InvoiceResponseDto response = InvoiceResponseDto.builder()
                .invoiceId(invoice.getId())
                .bookingId(invoice.getBookingId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .cloudinaryUrl(invoice.getCloudinaryUrl())
                .invoiceAmount(invoice.getInvoiceAmount())
                .createdAt(invoice.getCreatedAt())
                .build();

        log.info("Invoice retrieved: Invoice Number: {}, URL: {}",
                invoice.getInvoiceNumber(), invoice.getCloudinaryUrl());

        return ResponseEntity.ok(response);
    }
}

