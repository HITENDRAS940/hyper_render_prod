package com.hitendra.turf_booking_backend.dto.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.PaymentMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInventorySaleRequest {

    @NotNull(message = "Service ID is required")
    private Long serviceId;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    @NotNull(message = "Sale date is required")
    private LocalDate saleDate;

    private String customerName;

    private String notes;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<InventorySaleItemRequest> items;
}

