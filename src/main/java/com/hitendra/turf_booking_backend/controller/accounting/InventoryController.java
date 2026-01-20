package com.hitendra.turf_booking_backend.controller.accounting;

import com.hitendra.turf_booking_backend.dto.accounting.*;
import com.hitendra.turf_booking_backend.entity.accounting.*;
import com.hitendra.turf_booking_backend.service.accounting.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/accounting/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "Manage inventory items (Admin only)")
@PreAuthorize("hasRole('ADMIN')")
public class InventoryController {

    private final InventoryService inventoryService;

    // ==================== INVENTORY ITEMS ====================

    @PostMapping("/items")
    @Operation(summary = "Create inventory item", description = "Create a new inventory item")
    public ResponseEntity<InventoryItemDto> createItem(@Valid @RequestBody CreateInventoryItemRequest request) {
        InventoryItem item = inventoryService.createInventoryItem(request);
        return ResponseEntity.ok(mapItemToDto(item));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update inventory item", description = "Update an existing inventory item")
    public ResponseEntity<InventoryItemDto> updateItem(
        @PathVariable Long itemId,
        @Valid @RequestBody CreateInventoryItemRequest request
    ) {
        InventoryItem item = inventoryService.updateInventoryItem(itemId, request);
        return ResponseEntity.ok(mapItemToDto(item));
    }

    @PatchMapping("/items/{itemId}/toggle-active")
    @Operation(summary = "Toggle item active status", description = "Activate or deactivate an inventory item")
    public ResponseEntity<InventoryItemDto> toggleItemActiveStatus(
        @PathVariable Long itemId,
        @RequestParam Long serviceId
    ) {
        InventoryItem item = inventoryService.toggleItemActiveStatus(itemId, serviceId);
        return ResponseEntity.ok(mapItemToDto(item));
    }

    @GetMapping("/items/service/{serviceId}")
    @Operation(summary = "Get inventory items", description = "Get all inventory items for a service")
    public ResponseEntity<List<InventoryItemDto>> getInventoryItems(
        @PathVariable Long serviceId,
        @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        List<InventoryItem> items = inventoryService.getInventoryItems(serviceId, activeOnly);
        return ResponseEntity.ok(items.stream().map(this::mapItemToDto).collect(Collectors.toList()));
    }

    @GetMapping("/items/{itemId}")
    @Operation(summary = "Get inventory item", description = "Get a single inventory item by ID")
    public ResponseEntity<InventoryItemDto> getInventoryItem(
        @PathVariable Long itemId,
        @RequestParam Long serviceId
    ) {
        InventoryItem item = inventoryService.getInventoryItem(itemId, serviceId);
        return ResponseEntity.ok(mapItemToDto(item));
    }

    @GetMapping("/items/service/{serviceId}/low-stock")
    @Operation(summary = "Get low stock items", description = "Get items that are running low on stock")
    public ResponseEntity<List<InventoryItemDto>> getLowStockItems(@PathVariable Long serviceId) {
        List<InventoryItem> items = inventoryService.getLowStockItems(serviceId);
        return ResponseEntity.ok(items.stream().map(this::mapItemToDto).collect(Collectors.toList()));
    }

    @GetMapping("/items/service/{serviceId}/out-of-stock")
    @Operation(summary = "Get out of stock items", description = "Get items that are out of stock")
    public ResponseEntity<List<InventoryItemDto>> getOutOfStockItems(@PathVariable Long serviceId) {
        List<InventoryItem> items = inventoryService.getOutOfStockItems(serviceId);
        return ResponseEntity.ok(items.stream().map(this::mapItemToDto).collect(Collectors.toList()));
    }

    // ==================== INVENTORY PURCHASES ====================

    @PostMapping("/purchases")
    @Operation(summary = "Record purchase", description = "Record an inventory purchase (money OUT, stock IN)")
    public ResponseEntity<InventoryPurchaseDto> recordPurchase(@Valid @RequestBody CreateInventoryPurchaseRequest request) {
        InventoryPurchase purchase = inventoryService.recordPurchase(request);
        return ResponseEntity.ok(mapPurchaseToDto(purchase));
    }

    // ==================== INVENTORY SALES ====================

    @PostMapping("/sales")
    @Operation(summary = "Record sale", description = "Record an inventory sale (money IN, stock OUT)")
    public ResponseEntity<InventorySaleDto> recordSale(@Valid @RequestBody CreateInventorySaleRequest request) {
        InventorySale sale = inventoryService.recordSale(request);
        return ResponseEntity.ok(mapSaleToDto(sale));
    }

    // ==================== HELPER METHODS ====================

    private InventoryItemDto mapItemToDto(InventoryItem item) {
        boolean isLowStock = item.getMinStockLevel() != null && item.getStockQuantity() <= item.getMinStockLevel();
        boolean isOutOfStock = item.getStockQuantity() == 0;

        return InventoryItemDto.builder()
            .id(item.getId())
            .serviceId(item.getService().getId())
            .serviceName(item.getService().getName())
            .name(item.getName())
            .description(item.getDescription())
            .costPrice(item.getCostPrice())
            .sellingPrice(item.getSellingPrice())
            .stockQuantity(item.getStockQuantity())
            .minStockLevel(item.getMinStockLevel())
            .unit(item.getUnit())
            .active(item.getActive())
            .isLowStock(isLowStock)
            .isOutOfStock(isOutOfStock)
            .createdAt(item.getCreatedAt())
            .updatedAt(item.getUpdatedAt())
            .build();
    }

    private InventoryPurchaseDto mapPurchaseToDto(InventoryPurchase purchase) {
        List<InventoryPurchaseDto.InventoryPurchaseItemDto> itemDtos = purchase.getItems().stream()
            .map(item -> InventoryPurchaseDto.InventoryPurchaseItemDto.builder()
                .id(item.getId())
                .itemId(item.getItem().getId())
                .itemName(item.getItem().getName())
                .quantity(item.getQuantity())
                .costPrice(item.getCostPrice())
                .lineTotal(item.getLineTotal())
                .build())
            .collect(Collectors.toList());

        return InventoryPurchaseDto.builder()
            .id(purchase.getId())
            .serviceId(purchase.getService().getId())
            .serviceName(purchase.getService().getName())
            .totalAmount(purchase.getTotalAmount())
            .supplierName(purchase.getSupplierName())
            .supplierContact(purchase.getSupplierContact())
            .purchaseDate(purchase.getPurchaseDate())
            .paymentMode(purchase.getPaymentMode())
            .invoiceNumber(purchase.getInvoiceNumber())
            .notes(purchase.getNotes())
            .createdBy(purchase.getCreatedBy())
            .createdAt(purchase.getCreatedAt())
            .items(itemDtos)
            .build();
    }

    private InventorySaleDto mapSaleToDto(InventorySale sale) {
        List<InventorySaleDto.InventorySaleItemDto> itemDtos = sale.getItems().stream()
            .map(item -> InventorySaleDto.InventorySaleItemDto.builder()
                .id(item.getId())
                .itemId(item.getItem().getId())
                .itemName(item.getItem().getName())
                .quantity(item.getQuantity())
                .sellingPrice(item.getSellingPrice())
                .lineTotal(item.getLineTotal())
                .build())
            .collect(Collectors.toList());

        return InventorySaleDto.builder()
            .id(sale.getId())
            .serviceId(sale.getService().getId())
            .serviceName(sale.getService().getName())
            .totalAmount(sale.getTotalAmount())
            .paymentMode(sale.getPaymentMode())
            .saleDate(sale.getSaleDate())
            .soldBy(sale.getSoldBy())
            .customerName(sale.getCustomerName())
            .notes(sale.getNotes())
            .createdAt(sale.getCreatedAt())
            .items(itemDtos)
            .build();
    }
}

