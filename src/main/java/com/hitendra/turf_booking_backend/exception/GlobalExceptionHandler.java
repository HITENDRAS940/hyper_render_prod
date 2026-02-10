package com.hitendra.turf_booking_backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BookingException.class)
    public ResponseEntity<ErrorResponse> handleBookingException(BookingException ex) {
        log.error("Booking exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                ex.getHttpStatus(),
                ex.getMessage(),
                Instant.now(),
                ex.getErrorCode().name(),
                ex.getDetails()
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ErrorResponse> handlePaymentException(PaymentException ex) {
        log.error("Payment exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                ex.getHttpStatus(),
                ex.getMessage(),
                Instant.now(),
                ex.getErrorCode().name(),
                ex.getDetails()
        );
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<AuthErrorResponse> handleAuthException(AuthException ex) {
        log.error("Authentication exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        AuthErrorResponse error = new AuthErrorResponse(
                false,
                ex.getErrorCode().name(),
                ex.getMessage()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception occurred", ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                Instant.now(),
                "RUNTIME_ERROR",
                null
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed: " + errors.toString(),
                Instant.now(),
                "VALIDATION_ERROR",
                null
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected exception occurred", ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                Instant.now(),
                "INTERNAL_ERROR",
                null
        );
        return ResponseEntity.internalServerError().body(error);
    }

    public static class ErrorResponse {
        private int status;
        private String message;
        private Instant timestamp;
        private String errorCode;
        private String details;

        public ErrorResponse(int status, String message, Instant timestamp) {
            this(status, message, timestamp, null, null);
        }

        public ErrorResponse(int status, String message, Instant timestamp, String errorCode, String details) {
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
            this.errorCode = errorCode;
            this.details = details;
        }

        // Getters
        public int getStatus() { return status; }
        public String getMessage() { return message; }
        public Instant getTimestamp() { return timestamp; }
        public String getErrorCode() { return errorCode; }
        public String getDetails() { return details; }
    }

    /**
     * Custom error response for authentication errors
     */
    public static class AuthErrorResponse {
        private boolean success;
        private String errorCode;
        private String message;

        public AuthErrorResponse(boolean success, String errorCode, String message) {
            this.success = success;
            this.errorCode = errorCode;
            this.message = message;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
    }
}
