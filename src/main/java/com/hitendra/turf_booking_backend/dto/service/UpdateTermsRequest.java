package com.hitendra.turf_booking_backend.dto.service;

import lombok.Data;

/**
 * Request body used to create or replace the terms and conditions of a service.
 * The content field is intentionally unconstrained in length — it is stored in a
 * TEXT column and can hold many thousands of words.
 * Sending {@code null} clears any previously stored terms.
 */
@Data
public class UpdateTermsRequest {

    /**
     * Full terms and conditions text (plain text or Markdown).
     * Set to {@code null} to remove existing terms from the service.
     */
    private String termsAndConditions;
}

