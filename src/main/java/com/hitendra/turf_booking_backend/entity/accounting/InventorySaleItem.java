package com.hitendra.turf_booking_backend.entity.accounting;

import jakarta.persistence.*;
import lombok.*;

/**
 * Line items for inventory sales.
 * Links sale to individual items with quantity and selling price.
 */
@Entity
@Table(name = "inventory_sale_items", indexes = {
    @Index(name = "idx_sale_item", columnList = "sale_id, item_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private InventorySale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double sellingPrice; // Selling price at time of sale

    @Column(nullable = false)
    private Double lineTotal; // quantity * sellingPrice
}

