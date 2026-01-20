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
public class InventorySaleDto {

    private Long id;
    private Long serviceId;
    private String serviceName;
    private Double totalAmount;
    private PaymentMode paymentMode;
    private LocalDate saleDate;
    private String soldBy;
    private String customerName;
    private String notes;
    private Instant createdAt;
    private List<InventorySaleItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventorySaleItemDto {
        private Long id;
        private Long itemId;
        private String itemName;
        private Integer quantity;
        private Double sellingPrice;
        private Double lineTotal;
    }
}

