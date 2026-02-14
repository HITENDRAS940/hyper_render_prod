package com.hitendra.turf_booking_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Invoice entity for storing booking invoices.
 * One booking = one invoice (enforced by unique constraint on bookingId).
 */
@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoice_booking_id", columnList = "booking_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Booking ID (unique constraint - one invoice per booking).
     */
    @Column(name = "booking_id", nullable = false, unique = true)
    private Long bookingId;

    /**
     * Invoice number in format: INV-{bookingId}
     */
    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    /**
     * Cloudinary secure URL for the PDF invoice.
     */
    @Column(name = "cloudinary_url", nullable = false, length = 500)
    private String cloudinaryUrl;

    /**
     * Public ID in Cloudinary (for deletion if needed).
     */
    @Column(name = "cloudinary_public_id", length = 255)
    private String cloudinaryPublicId;

    /**
     * Invoice creation timestamp.
     */
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Invoice amount (same as booking amount for reference).
     */
    @Column(name = "invoice_amount", nullable = false)
    private Double invoiceAmount;

    /**
     * Template version used to generate this invoice.
     * Allows tracking which template was used for historical invoices.
     */
    @Column(name = "template_version")
    private Integer templateVersion;
}

