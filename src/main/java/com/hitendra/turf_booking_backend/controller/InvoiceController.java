package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.invoice.InvoiceReceiveRequest;
import com.hitendra.turf_booking_backend.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Invoice", description = "Invoice callback and retrieval APIs")
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Callback endpoint called by the invoice generator service after generating an invoice.
     * No authentication required — called server-to-server by the invoice generator.
     */
    @PostMapping("/invoice-receive")
    @Operation(summary = "Receive Invoice", description = "Called by invoice generator service to deliver the generated invoice URL")
    public ResponseEntity<Map<String, String>> receiveInvoice(@Valid @RequestBody InvoiceReceiveRequest request) {
        log.info("📥 Invoice received callback for booking ID: {}", request.getBookingId());
        invoiceService.receiveInvoice(request);
        return ResponseEntity.ok(Map.of("message", "Invoice received and saved successfully"));
    }
}

