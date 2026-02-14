package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.invoice.InvoiceResponseDto;
import com.hitendra.turf_booking_backend.entity.Booking;
import com.hitendra.turf_booking_backend.entity.Invoice;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import com.hitendra.turf_booking_backend.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for invoice management.
 *
 * FEATURE FLAG:
 * - This controller is ONLY created if invoice.generation.enabled=true
 * - If disabled, invoice endpoints will return 404
 * - Keeps application lightweight for free-tier deployments
 *
 * Provides API to fetch invoice URLs for confirmed bookings.
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Invoice Management", description = "APIs for fetching booking invoices")
@ConditionalOnProperty(
    prefix = "invoice.generation",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final BookingRepository bookingRepository;

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

    /**
     * Manually regenerate invoice for a confirmed booking (ADMIN/MANAGER only).
     *
     * Use this endpoint to:
     * - Recover from failed async invoice generation
     * - Regenerate invoice with updated template
     * - Debug invoice generation issues
     *
     * @param bookingId Booking ID
     * @return InvoiceResponseDto with new invoice details
     */
    @PostMapping("/regenerate/{bookingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(
        summary = "Manually regenerate invoice (ADMIN/MANAGER only)",
        description = """
            Manually triggers invoice generation for a confirmed booking.
            
            **Use Cases:**
            - Recover from failed async invoice generation
            - Regenerate invoice with updated template
            - Debug invoice generation issues
            
            **Note:** This will NOT create duplicate invoices. If invoice already exists,
            it returns the existing one (idempotent).
            
            **Requirements:**
            - Booking must exist and be CONFIRMED
            - Invoice template must be configured
            - Cloudinary must be configured
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invoice generated/retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin/Manager role required"),
        @ApiResponse(responseCode = "404", description = "Booking not found"),
        @ApiResponse(responseCode = "500", description = "Invoice generation failed")
    })
    public ResponseEntity<InvoiceResponseDto> regenerateInvoice(
            @Parameter(description = "Booking ID", required = true)
            @PathVariable Long bookingId) {

        log.info("🔄 Manual invoice regeneration requested for booking ID: {}", bookingId);

        // Fetch booking with all relationships eagerly loaded
        // This prevents LazyInitializationException during invoice generation
        Booking booking = bookingRepository.findByIdWithAllRelationships(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        log.info("📦 Booking found with all relationships: ID={}, Status={}, Amount={}",
                booking.getId(), booking.getStatus(), booking.getAmount());

        // Generate invoice (idempotent - returns existing if already generated)
        invoiceService.generateAndStoreInvoice(booking);

        // Fetch the saved invoice
        Invoice invoice = invoiceService.getInvoiceByBookingId(bookingId);

        InvoiceResponseDto response = InvoiceResponseDto.builder()
                .invoiceId(invoice.getId())
                .bookingId(invoice.getBookingId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .cloudinaryUrl(invoice.getCloudinaryUrl())
                .invoiceAmount(invoice.getInvoiceAmount())
                .createdAt(invoice.getCreatedAt())
                .build();

        log.info("✅ Invoice regeneration completed: Invoice Number: {}, URL: {}",
                invoice.getInvoiceNumber(), invoice.getCloudinaryUrl());

        return ResponseEntity.ok(response);
    }
}

