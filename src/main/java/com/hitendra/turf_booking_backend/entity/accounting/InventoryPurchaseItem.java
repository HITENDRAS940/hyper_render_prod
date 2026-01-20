package com.hitendra.turf_booking_backend.entity.accounting;

import jakarta.persistence.*;
import lombok.*;

/**
 * Line items for inventory purchases.
 * Links purchase to individual items with quantity and cost.
 */
@Entity
@Table(name = "inventory_purchase_items", indexes = {
    @Index(name = "idx_purchase_item", columnList = "purchase_id, item_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryPurchaseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private InventoryPurchase purchase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double costPrice; // Cost at time of purchase (may vary)

    @Column(nullable = false)
    private Double lineTotal; // quantity * costPrice
}

