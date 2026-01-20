package com.hitendra.turf_booking_backend.exception;

/**
 * Exception thrown when wallet operation fails.
 */
public class WalletException extends RuntimeException {

    private final WalletErrorCode errorCode;

    public WalletException(String message) {
        super(message);
        this.errorCode = WalletErrorCode.WALLET_ERROR;
    }

    public WalletException(String message, WalletErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public WalletException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = WalletErrorCode.WALLET_ERROR;
    }

    public WalletException(String message, WalletErrorCode errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public WalletErrorCode getErrorCode() {
        return errorCode;
    }

    public enum WalletErrorCode {
        WALLET_NOT_FOUND,
        WALLET_BLOCKED,
        INSUFFICIENT_BALANCE,
        DUPLICATE_TRANSACTION,
        INVALID_AMOUNT,
        WALLET_ERROR
    }
}

