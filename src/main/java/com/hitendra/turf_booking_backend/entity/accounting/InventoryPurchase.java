package com.hitendra.turf_booking_backend.entity.accounting;

import com.hitendra.turf_booking_backend.entity.Service;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Records inventory purchases (OUTGOING money).
 * When items are bought from suppliers.
 *
 * FLOW:
 * 1. Create InventoryPurchase
 * 2. Add InventoryPurchaseItems
 * 3. Increase stock in InventoryItem
 * 4. Create Expense entry
 * 5. Create CashLedger debit entry
 */
@Entity
@Table(name = "inventory_purchases", indexes = {
    @Index(name = "idx_service_purchase_date", columnList = "service_id, purchase_date"),
    @Index(name = "idx_purchase_date", columnList = "purchase_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(nullable = false)
    private Double totalAmount;

    @Column
    private String supplierName;

    @Column
    private String supplierContact;

    @Column(nullable = false)
    private LocalDate purchaseDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode paymentMode;

    @Column
    private String invoiceNumber;

    @Column
    private String notes;

    @Column
    private String createdBy; // Admin who recorded this

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InventoryPurchaseItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

