package com.hitendra.turf_booking_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * UPI Payment Configuration
 *
 * Loads UPI payment details from application.properties
 * Used for manual UPI payment bookings
 *
 * Properties:
 * - upi.payment.id: Payee UPI ID (e.g., turfbooking@bankname)
 * - upi.payment.merchant-name: Merchant/Payee name (exact bank-registered name)
 * - upi.payment.currency: Currency code (default: INR)
 */
@Data
@Component
@ConfigurationProperties(prefix = "upi.payment")
public class UpiPaymentConfig {

    /**
     * Payee UPI ID
     * Example: turfbooking@bankname, admin@upi, etc.
     */
    private String id;

    /**
     * Merchant/Payee Name
     * Must be exact bank-registered name
     * Used in UPI payment confirmation
     */
    private String merchantName;

    /**
     * Currency Code
     * Default: INR (Indian Rupee)
     * Only INR is supported currently
     */
    private String currency = "INR";
}

