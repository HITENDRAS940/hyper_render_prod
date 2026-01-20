package com.hitendra.turf_booking_backend.entity.accounting;

import com.hitendra.turf_booking_backend.entity.Service;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Master table for inventory items (sellable products).
 * Examples: Coke, Chips, Water, Sports equipment for rent, etc.
 */
@Entity
@Table(name = "inventory_items", indexes = {
    @Index(name = "idx_service_inventory", columnList = "service_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private Double costPrice; // What we pay to buy

    @Column(nullable = false)
    private Double sellingPrice; // What we sell for

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column
    private Integer minStockLevel; // Alert threshold for low stock

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryUnit unit; // PIECE, KG, LITER, etc.

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (active == null) {
            active = true;
        }
        if (stockQuantity == null) {
            stockQuantity = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

