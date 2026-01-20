package com.hitendra.turf_booking_backend.dto.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.InventoryUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemDto {

    private Long id;
    private Long serviceId;
    private String serviceName;
    private String name;
    private String description;
    private Double costPrice;
    private Double sellingPrice;
    private Integer stockQuantity;
    private Integer minStockLevel;
    private InventoryUnit unit;
    private Boolean active;
    private Boolean isLowStock;
    private Boolean isOutOfStock;
    private Instant createdAt;
    private Instant updatedAt;
}

