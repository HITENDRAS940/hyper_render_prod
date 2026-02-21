package com.hitendra.turf_booking_backend.dto.invoice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceReceiveRequest {

    @NotNull(message = "bookingId is required")
    private Long bookingId;

    @NotBlank(message = "invoiceURL is required")
    private String invoiceURL;
}

