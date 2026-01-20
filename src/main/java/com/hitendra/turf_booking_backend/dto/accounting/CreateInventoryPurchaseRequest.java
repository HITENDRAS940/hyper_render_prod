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
public class CreateInventoryPurchaseRequest {

    @NotNull(message = "Service ID is required")
    private Long serviceId;

    private String supplierName;

    private String supplierContact;

    @NotNull(message = "Purchase date is required")
    private LocalDate purchaseDate;

    @NotNull(message = "Payment mode is required")
    private PaymentMode paymentMode;

    private String invoiceNumber;

    private String notes;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<InventoryPurchaseItemRequest> items;
}

