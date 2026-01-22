package com.hitendra.turf_booking_backend.entity;

/**
 * Account status for GDPR/Privacy Policy compliance.
 * Used to track the lifecycle of a user account.
 */
public enum AccountStatus {
    /**
     * Active account - user can login and use the app
     */
    ACTIVE,

    /**
     * Account is suspended - user cannot login but data is preserved
     */
    SUSPENDED,

    /**
     * Account is deleted - all PII has been permanently removed
     * Only non-identifiable placeholder remains for referential integrity
     * User cannot be re-identified from remaining data
     */
    DELETED
}
