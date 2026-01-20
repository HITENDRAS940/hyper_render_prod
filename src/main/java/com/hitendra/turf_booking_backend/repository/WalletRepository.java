package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.Wallet;
import com.hitendra.turf_booking_backend.entity.WalletStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUserId(Long userId);

    /**
     * Find wallet by user ID with pessimistic write lock.
     * Use this for all balance update operations to prevent concurrent modifications.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
    Optional<Wallet> findByUserIdWithLock(@Param("userId") Long userId);

    /**
     * Find wallet by ID with pessimistic write lock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000")})
    @Query("SELECT w FROM Wallet w WHERE w.id = :walletId")
    Optional<Wallet> findByIdWithLock(@Param("walletId") Long walletId);

    boolean existsByUserId(Long userId);

    @Query("SELECT w FROM Wallet w WHERE w.user.id = :userId AND w.status = :status")
    Optional<Wallet> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") WalletStatus status);
}

