package com.hitendra.turf_booking_backend.dto.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.PaymentMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryPurchaseDto {

    private Long id;
    private Long serviceId;
    private String serviceName;
    private Double totalAmount;
    private String supplierName;
    private String supplierContact;
    private LocalDate purchaseDate;
    private PaymentMode paymentMode;
    private String invoiceNumber;
    private String notes;
    private String createdBy;
    private Instant createdAt;
    private List<InventoryPurchaseItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryPurchaseItemDto {
        private Long id;
        private Long itemId;
        private String itemName;
        private Integer quantity;
        private Double costPrice;
        private Double lineTotal;
    }
}

