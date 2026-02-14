package com.hitendra.turf_booking_backend.exception;

/**
 * Exception thrown when invoice generation or retrieval fails.
 */
public class InvoiceException extends RuntimeException {

    public InvoiceException(String message) {
        super(message);
    }

    public InvoiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

