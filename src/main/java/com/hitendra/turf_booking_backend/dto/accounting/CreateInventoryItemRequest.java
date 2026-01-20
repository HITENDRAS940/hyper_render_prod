package com.hitendra.turf_booking_backend.dto.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.InventoryUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInventoryItemRequest {

    @NotNull(message = "Service ID is required")
    private Long serviceId;

    @NotBlank(message = "Item name is required")
    private String name;

    private String description;

    @NotNull(message = "Cost price is required")
    @Positive(message = "Cost price must be positive")
    private Double costPrice;

    @NotNull(message = "Selling price is required")
    @Positive(message = "Selling price must be positive")
    private Double sellingPrice;

    @PositiveOrZero(message = "Stock quantity cannot be negative")
    private Integer stockQuantity;

    @PositiveOrZero(message = "Min stock level cannot be negative")
    private Integer minStockLevel;

    @NotNull(message = "Unit is required")
    private InventoryUnit unit;
}

