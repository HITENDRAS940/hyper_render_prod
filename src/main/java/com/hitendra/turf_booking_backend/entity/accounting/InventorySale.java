package com.hitendra.turf_booking_backend.entity.accounting;

import com.hitendra.turf_booking_backend.entity.Service;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Records inventory sales (INCOMING money).
 * When items are sold to customers.
 *
 * FLOW:
 * 1. Create InventorySale
 * 2. Add InventorySaleItems
 * 3. Decrease stock in InventoryItem
 * 4. Create CashLedger credit entry
 */
@Entity
@Table(name = "inventory_sales", indexes = {
    @Index(name = "idx_service_sale_date", columnList = "service_id, sale_date"),
    @Index(name = "idx_sale_date", columnList = "sale_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(nullable = false)
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode paymentMode;

    @Column(nullable = false)
    private LocalDate saleDate;

    @Column
    private String soldBy; // Staff/Admin who made the sale

    @Column
    private String customerName;

    @Column
    private String notes;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InventorySaleItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}

