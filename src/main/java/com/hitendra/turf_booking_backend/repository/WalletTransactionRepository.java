package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.WalletTransaction;
import com.hitendra.turf_booking_backend.entity.WalletTransactionSource;
import com.hitendra.turf_booking_backend.entity.WalletTransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /**
     * Check if a transaction with the given source and reference already exists.
     * Used for idempotency.
     */
    boolean existsBySourceAndReferenceId(WalletTransactionSource source, String referenceId);

    /**
     * Find transaction by source and reference.
     */
    Optional<WalletTransaction> findBySourceAndReferenceId(WalletTransactionSource source, String referenceId);

    /**
     * Find all transactions for a wallet.
     */
    Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    /**
     * Find all transactions for a wallet by status.
     */
    List<WalletTransaction> findByWalletIdAndStatusOrderByCreatedAtDesc(Long walletId, WalletTransactionStatus status);

    /**
     * Find pending transactions by reference (for webhook reconciliation).
     */
    @Query("SELECT t FROM WalletTransaction t WHERE t.referenceId = :referenceId AND t.status = :status")
    Optional<WalletTransaction> findByReferenceIdAndStatus(
            @Param("referenceId") String referenceId,
            @Param("status") WalletTransactionStatus status);

    /**
     * Find all transactions by reference ID.
     */
    List<WalletTransaction> findByReferenceId(String referenceId);

    /**
     * Count successful transactions for a reference (idempotency check).
     */
    @Query("SELECT COUNT(t) FROM WalletTransaction t WHERE t.referenceId = :referenceId AND t.status = 'SUCCESS'")
    long countSuccessfulByReferenceId(@Param("referenceId") String referenceId);
}

