package com.hitendra.turf_booking_backend.service.accounting;

import com.hitendra.turf_booking_backend.dto.accounting.*;
import com.hitendra.turf_booking_backend.entity.Service;
import com.hitendra.turf_booking_backend.entity.accounting.*;
import com.hitendra.turf_booking_backend.exception.BookingException;
import com.hitendra.turf_booking_backend.repository.ServiceRepository;
import com.hitendra.turf_booking_backend.repository.accounting.*;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * INVENTORY SERVICE - Manages Inventory Purchases and Sales
 * ═══════════════════════════════════════════════════════════════════════════
 *
 * PURCHASE FLOW:
 * 1. Create InventoryPurchase with items
 * 2. Increase stock for each item
 * 3. Create Expense entry
 * 4. Record DEBIT in cash ledger
 *
 * SALE FLOW:
 * 1. Check stock availability
 * 2. Create InventorySale with items
 * 3. Decrease stock for each item
 * 4. Record CREDIT in cash ledger
 *
 * IMPORTANT: Stock changes and ledger entries must be atomic.
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryItemRepository itemRepository;
    private final InventoryPurchaseRepository purchaseRepository;
    private final InventorySaleRepository saleRepository;
    private final ServiceRepository serviceRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final LedgerService ledgerService;
    private final AuthUtil authUtil;

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * RECORD INVENTORY PURCHASE (Money OUT)
     * ═══════════════════════════════════════════════════════════════════════════
     *
     * TRANSACTION STEPS:
     * 1. Validate service and items
     * 2. Create purchase record with line items
     * 3. Increase stock quantities
     * 4. Create expense entry
     * 5. Record debit in cash ledger
     *
     * @param request Purchase details
     * @return Created purchase record
     */
    @Transactional
    public InventoryPurchase recordPurchase(CreateInventoryPurchaseRequest request) {
        log.info("Recording inventory purchase for service {}", request.getServiceId());

        // Validate service
        Service service = serviceRepository.findById(request.getServiceId())
            .orElseThrow(() -> new BookingException("Service not found: " + request.getServiceId()));

        String currentUser = authUtil.getCurrentUser().getPhone();

        // Calculate total amount
        double totalAmount = 0.0;
        List<InventoryPurchaseItem> purchaseItems = new ArrayList<>();

        // Create purchase record
        InventoryPurchase purchase = InventoryPurchase.builder()
            .service(service)
            .supplierName(request.getSupplierName())
            .supplierContact(request.getSupplierContact())
            .purchaseDate(request.getPurchaseDate())
            .paymentMode(request.getPaymentMode())
            .invoiceNumber(request.getInvoiceNumber())
            .notes(request.getNotes())
            .createdBy(currentUser)
            .build();

        // Save purchase first to get ID
        InventoryPurchase savedPurchase = purchaseRepository.save(purchase);

        // Process each item
        for (InventoryPurchaseItemRequest itemReq : request.getItems()) {
            InventoryItem item = itemRepository.findByIdAndServiceId(itemReq.getItemId(), service.getId())
                .orElseThrow(() -> new BookingException("Item not found: " + itemReq.getItemId()));

            double lineTotal = itemReq.getQuantity() * itemReq.getCostPrice();
            totalAmount += lineTotal;

            // Create purchase line item
            InventoryPurchaseItem purchaseItem = InventoryPurchaseItem.builder()
                .purchase(savedPurchase)
                .item(item)
                .quantity(itemReq.getQuantity())
                .costPrice(itemReq.getCostPrice())
                .lineTotal(lineTotal)
                .build();

            purchaseItems.add(purchaseItem);

            // INCREASE STOCK
            item.setStockQuantity(item.getStockQuantity() + itemReq.getQuantity());
            itemRepository.save(item);

            log.info("Stock increased for item {}: +{} (new stock: {})",
                item.getName(), itemReq.getQuantity(), item.getStockQuantity());
        }

        savedPurchase.setTotalAmount(totalAmount);
        savedPurchase.setItems(purchaseItems);
        savedPurchase = purchaseRepository.save(savedPurchase);

        log.info("Inventory purchase created: ID={}, Total={}", savedPurchase.getId(), totalAmount);

        // Create expense entry for this purchase
        ExpenseCategory inventoryCategory = categoryRepository.findByName("Inventory Purchase")
            .orElseThrow(() -> new BookingException("Inventory Purchase category not found"));

        Expense expense = Expense.builder()
            .service(service)
            .category(inventoryCategory)
            .description("Inventory Purchase" +
                (request.getSupplierName() != null ? " from " + request.getSupplierName() : ""))
            .amount(totalAmount)
            .paymentMode(request.getPaymentMode())
            .expenseDate(request.getPurchaseDate())
            .referenceNumber("PURCHASE-" + savedPurchase.getId())
            .createdBy(currentUser)
            .build();

        expenseRepository.save(expense);

        // Record DEBIT in ledger (money OUT)
        ledgerService.recordDebit(
            service,
            LedgerSource.INVENTORY_PURCHASE,
            ReferenceType.INVENTORY_PURCHASE,
            savedPurchase.getId(),
            totalAmount,
            request.getPaymentMode(),
            "Inventory Purchase: " + purchaseItems.size() + " items" +
                (request.getSupplierName() != null ? " from " + request.getSupplierName() : ""),
            currentUser
        );

        log.info("Inventory purchase recorded in ledger successfully");

        return savedPurchase;
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * RECORD INVENTORY SALE (Money IN)
     * ═══════════════════════════════════════════════════════════════════════════
     *
     * TRANSACTION STEPS:
     * 1. Validate service and items
     * 2. Check stock availability
     * 3. Create sale record with line items
     * 4. Decrease stock quantities
     * 5. Record credit in cash ledger
     *
     * @param request Sale details
     * @return Created sale record
     */
    @Transactional
    public InventorySale recordSale(CreateInventorySaleRequest request) {
        log.info("Recording inventory sale for service {}", request.getServiceId());

        // Validate service
        Service service = serviceRepository.findById(request.getServiceId())
            .orElseThrow(() -> new BookingException("Service not found: " + request.getServiceId()));

        String currentUser = authUtil.getCurrentUser().getPhone();

        // STEP 1: Validate stock availability BEFORE creating sale
        for (InventorySaleItemRequest itemReq : request.getItems()) {
            InventoryItem item = itemRepository.findByIdAndServiceId(itemReq.getItemId(), service.getId())
                .orElseThrow(() -> new BookingException("Item not found: " + itemReq.getItemId()));

            if (item.getStockQuantity() < itemReq.getQuantity()) {
                throw new BookingException("Insufficient stock for item '" + item.getName() +
                    "'. Available: " + item.getStockQuantity() + ", Requested: " + itemReq.getQuantity());
            }
        }

        // Calculate total amount
        double totalAmount = 0.0;
        List<InventorySaleItem> saleItems = new ArrayList<>();

        // Create sale record
        InventorySale sale = InventorySale.builder()
            .service(service)
            .paymentMode(request.getPaymentMode())
            .saleDate(request.getSaleDate())
            .soldBy(currentUser)
            .customerName(request.getCustomerName())
            .notes(request.getNotes())
            .build();

        // Save sale first to get ID
        InventorySale savedSale = saleRepository.save(sale);

        // Process each item
        for (InventorySaleItemRequest itemReq : request.getItems()) {
            InventoryItem item = itemRepository.findByIdAndServiceId(itemReq.getItemId(), service.getId())
                .orElseThrow(() -> new BookingException("Item not found: " + itemReq.getItemId()));

            double lineTotal = itemReq.getQuantity() * item.getSellingPrice();
            totalAmount += lineTotal;

            // Create sale line item
            InventorySaleItem saleItem = InventorySaleItem.builder()
                .sale(savedSale)
                .item(item)
                .quantity(itemReq.getQuantity())
                .sellingPrice(item.getSellingPrice())
                .lineTotal(lineTotal)
                .build();

            saleItems.add(saleItem);

            // DECREASE STOCK
            item.setStockQuantity(item.getStockQuantity() - itemReq.getQuantity());
            itemRepository.save(item);

            log.info("Stock decreased for item {}: -{} (new stock: {})",
                item.getName(), itemReq.getQuantity(), item.getStockQuantity());
        }

        savedSale.setTotalAmount(totalAmount);
        savedSale.setItems(saleItems);
        savedSale = saleRepository.save(savedSale);

        log.info("Inventory sale created: ID={}, Total={}", savedSale.getId(), totalAmount);

        // Record CREDIT in ledger (money IN)
        ledgerService.recordCredit(
            service,
            LedgerSource.INVENTORY_SALE,
            ReferenceType.INVENTORY_SALE,
            savedSale.getId(),
            totalAmount,
            request.getPaymentMode(),
            "Inventory Sale: " + saleItems.size() + " items" +
                (request.getCustomerName() != null ? " to " + request.getCustomerName() : ""),
            currentUser
        );

        log.info("Inventory sale recorded in ledger successfully");

        return savedSale;
    }

    /**
     * Get low stock items for alerts.
     */
    public List<InventoryItem> getLowStockItems(Long serviceId) {
        return itemRepository.findLowStockItems(serviceId);
    }

    /**
     * Get out of stock items.
     */
    public List<InventoryItem> getOutOfStockItems(Long serviceId) {
        return itemRepository.findOutOfStockItems(serviceId);
    }

    /**
     * Create a new inventory item.
     */
    @Transactional
    public InventoryItem createInventoryItem(CreateInventoryItemRequest request) {
        log.info("Creating inventory item for service {}: {}", request.getServiceId(), request.getName());

        Service service = serviceRepository.findById(request.getServiceId())
            .orElseThrow(() -> new BookingException("Service not found: " + request.getServiceId()));

        InventoryItem item = InventoryItem.builder()
            .service(service)
            .name(request.getName())
            .description(request.getDescription())
            .costPrice(request.getCostPrice())
            .sellingPrice(request.getSellingPrice())
            .stockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0)
            .minStockLevel(request.getMinStockLevel() != null ? request.getMinStockLevel() : 0)
            .unit(request.getUnit())
            .active(true)
            .build();

        InventoryItem saved = itemRepository.save(item);
        log.info("Inventory item created: ID={}, Name={}", saved.getId(), saved.getName());

        return saved;
    }

    /**
     * Update an inventory item.
     */
    @Transactional
    public InventoryItem updateInventoryItem(Long itemId, CreateInventoryItemRequest request) {
        log.info("Updating inventory item {}", itemId);

        InventoryItem item = itemRepository.findByIdAndServiceId(itemId, request.getServiceId())
            .orElseThrow(() -> new BookingException("Item not found: " + itemId));

        item.setName(request.getName());
        item.setDescription(request.getDescription());
        item.setCostPrice(request.getCostPrice());
        item.setSellingPrice(request.getSellingPrice());
        item.setMinStockLevel(request.getMinStockLevel() != null ? request.getMinStockLevel() : 0);
        item.setUnit(request.getUnit());

        InventoryItem updated = itemRepository.save(item);
        log.info("Inventory item updated: ID={}", updated.getId());

        return updated;
    }

    /**
     * Toggle inventory item active status.
     */
    @Transactional
    public InventoryItem toggleItemActiveStatus(Long itemId, Long serviceId) {
        InventoryItem item = itemRepository.findByIdAndServiceId(itemId, serviceId)
            .orElseThrow(() -> new BookingException("Item not found: " + itemId));

        item.setActive(!item.getActive());
        return itemRepository.save(item);
    }

    /**
     * Get all inventory items for a service.
     */
    public List<InventoryItem> getInventoryItems(Long serviceId, boolean activeOnly) {
        return activeOnly
            ? itemRepository.findByServiceIdAndActiveTrue(serviceId)
            : itemRepository.findByServiceId(serviceId);
    }

    /**
     * Get single inventory item.
     */
    public InventoryItem getInventoryItem(Long itemId, Long serviceId) {
        return itemRepository.findByIdAndServiceId(itemId, serviceId)
            .orElseThrow(() -> new BookingException("Item not found: " + itemId));
    }
}

