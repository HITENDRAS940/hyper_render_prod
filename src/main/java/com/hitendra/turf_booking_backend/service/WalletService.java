package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.wallet.*;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.exception.WalletException;
import com.hitendra.turf_booking_backend.exception.WalletException.WalletErrorCode;
import com.hitendra.turf_booking_backend.repository.UserRepository;
import com.hitendra.turf_booking_backend.repository.WalletRepository;
import com.hitendra.turf_booking_backend.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service for wallet operations.
 * All balance-changing operations use pessimistic locking and are transactional.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    // ==================== Wallet Management ====================

    /**
     * Get wallet for a user.
     * Wallet should already exist as it's created during user registration.
     *
     * @param userId The user ID
     * @return The wallet
     * @throws WalletException if wallet not found
     */
    @Transactional(readOnly = true)
    public Wallet getWallet(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletException(
                        "Wallet not found for user: " + userId + ". This should not happen - wallet is created during registration.",
                        WalletErrorCode.WALLET_NOT_FOUND));
    }

    /**
     * Get wallet for a user, returning Optional.
     * Use this when you need to check if wallet exists without throwing.
     *
     * @param userId The user ID
     * @return Optional containing the wallet if exists
     */
    @Transactional(readOnly = true)
    public java.util.Optional<Wallet> findWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId);
    }

    /**
     * Create a new wallet for user.
     *
     * IMPORTANT: This should ONLY be called from UserRegistrationService during user registration.
     * Wallet creation is an internal operation that must happen atomically with user creation.
     *
     * @param userId The user ID
     * @return The created wallet
     * @throws WalletException if wallet already exists or user not found
     */
    @Transactional
    public Wallet createWalletForUser(Long userId) {
        // Double-check that wallet doesn't already exist (safety against concurrent requests)
        if (walletRepository.existsByUserId(userId)) {
            log.warn("Attempted to create duplicate wallet for user: {}", userId);
            throw new WalletException("Wallet already exists for user: " + userId, WalletErrorCode.WALLET_ERROR);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WalletException("User not found: " + userId, WalletErrorCode.WALLET_NOT_FOUND));

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build();

        Wallet saved = walletRepository.save(wallet);
        log.info("Created wallet for user {}: walletId={}", userId, saved.getId());
        return saved;
    }

    /**
     * Get wallet DTO for current user.
     * Wallet must already exist (created during registration).
     */
    @Transactional(readOnly = true)
    public WalletDto getWalletDto(Long userId) {
        Wallet wallet = getWallet(userId);
        return toDto(wallet);
    }

    /**
     * Get wallet balance for user.
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(Wallet::getBalance)
                .orElse(BigDecimal.ZERO);
    }

    // ==================== Credit Operations ====================

    /**
     * Credit amount to wallet with pessimistic locking.
     * Used for: top-up, refund, cashback, admin credit.
     *
     * @param userId      User ID
     * @param amount      Amount to credit (positive)
     * @param source      Source of the credit
     * @param referenceId External reference for idempotency
     * @param description Optional description
     * @return The wallet transaction
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public WalletTransaction creditWallet(Long userId, BigDecimal amount,
                                          WalletTransactionSource source,
                                          String referenceId, String description) {

        validateAmount(amount);

        // Idempotency check: if a successful transaction exists with same source+reference, skip
        if (referenceId != null && walletTransactionRepository.existsBySourceAndReferenceId(source, referenceId)) {
            WalletTransaction existing = walletTransactionRepository.findBySourceAndReferenceId(source, referenceId)
                    .orElse(null);
            if (existing != null && existing.getStatus() == WalletTransactionStatus.SUCCESS) {
                log.warn("Duplicate credit attempt: source={}, referenceId={}, returning existing transaction",
                        source, referenceId);
                return existing;
            }
        }

        // Lock wallet for update - wallet must already exist
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new WalletException(
                        "Wallet not found for user: " + userId + ". Wallet must be created during registration.",
                        WalletErrorCode.WALLET_NOT_FOUND));

        validateWalletActive(wallet);

        // Credit the wallet
        wallet.credit(amount);

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(WalletTransactionType.CREDIT)
                .source(source)
                .referenceId(referenceId)
                .status(WalletTransactionStatus.SUCCESS)
                .description(description)
                .build();

        walletRepository.save(wallet);
        WalletTransaction savedTx = walletTransactionRepository.save(transaction);

        log.info("Credited wallet: userId={}, amount={}, source={}, referenceId={}, newBalance={}",
                userId, amount, source, referenceId, wallet.getBalance());

        return savedTx;
    }

    // ==================== Debit Operations ====================

    /**
     * Debit amount from wallet with pessimistic locking.
     * Used for: booking payment.
     *
     * @param userId      User ID
     * @param amount      Amount to debit (positive)
     * @param source      Source of the debit
     * @param referenceId External reference for idempotency
     * @param description Optional description
     * @return The wallet transaction
     * @throws WalletException if insufficient balance or wallet blocked
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public WalletTransaction debitWallet(Long userId, BigDecimal amount,
                                         WalletTransactionSource source,
                                         String referenceId, String description) {

        validateAmount(amount);

        // Idempotency check
        if (referenceId != null && walletTransactionRepository.existsBySourceAndReferenceId(source, referenceId)) {
            WalletTransaction existing = walletTransactionRepository.findBySourceAndReferenceId(source, referenceId)
                    .orElse(null);
            if (existing != null && existing.getStatus() == WalletTransactionStatus.SUCCESS) {
                log.warn("Duplicate debit attempt: source={}, referenceId={}, returning existing transaction",
                        source, referenceId);
                return existing;
            }
        }

        // Lock wallet for update
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new WalletException("Wallet not found for user: " + userId,
                        WalletErrorCode.WALLET_NOT_FOUND));

        validateWalletActive(wallet);

        // Check sufficient balance
        if (wallet.getBalance().compareTo(amount) < 0) {
            log.warn("Insufficient balance: userId={}, required={}, available={}",
                    userId, amount, wallet.getBalance());
            throw new WalletException("Insufficient wallet balance",
                    WalletErrorCode.INSUFFICIENT_BALANCE);
        }

        // Debit the wallet
        wallet.debit(amount);

        // Create transaction record
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(WalletTransactionType.DEBIT)
                .source(source)
                .referenceId(referenceId)
                .status(WalletTransactionStatus.SUCCESS)
                .description(description)
                .build();

        walletRepository.save(wallet);
        WalletTransaction savedTx = walletTransactionRepository.save(transaction);

        log.info("Debited wallet: userId={}, amount={}, source={}, referenceId={}, newBalance={}",
                userId, amount, source, referenceId, wallet.getBalance());

        return savedTx;
    }

    /**
     * Check if user has sufficient wallet balance.
     */
    @Transactional(readOnly = true)
    public boolean hasSufficientBalance(Long userId, BigDecimal amount) {
        return walletRepository.findByUserId(userId)
                .map(w -> w.getBalance().compareTo(amount) >= 0)
                .orElse(false);
    }

    // ==================== Pending Transaction Operations ====================

    /**
     * Create a PENDING wallet transaction (for top-up before payment confirmation).
     * Wallet balance is NOT updated until confirmPendingTransaction is called.
     * Wallet must already exist (created during registration).
     */
    @Transactional
    public WalletTransaction createPendingCreditTransaction(Long userId, BigDecimal amount,
                                                             WalletTransactionSource source,
                                                             String referenceId, String description) {
        validateAmount(amount);

        Wallet wallet = getWallet(userId);

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(WalletTransactionType.CREDIT)
                .source(source)
                .referenceId(referenceId)
                .status(WalletTransactionStatus.PENDING)
                .description(description)
                .build();

        WalletTransaction saved = walletTransactionRepository.save(transaction);
        log.info("Created pending credit transaction: userId={}, amount={}, referenceId={}",
                userId, amount, referenceId);
        return saved;
    }

    /**
     * Confirm a pending transaction and update wallet balance.
     * Called after webhook verification.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public WalletTransaction confirmPendingTransaction(String referenceId) {
        WalletTransaction transaction = walletTransactionRepository
                .findByReferenceIdAndStatus(referenceId, WalletTransactionStatus.PENDING)
                .orElseThrow(() -> new WalletException("Pending transaction not found: " + referenceId,
                        WalletErrorCode.WALLET_ERROR));

        // Check if already processed (idempotency)
        long successCount = walletTransactionRepository.countSuccessfulByReferenceId(referenceId);
        if (successCount > 0) {
            log.warn("Transaction already confirmed: referenceId={}", referenceId);
            return walletTransactionRepository.findBySourceAndReferenceId(
                    transaction.getSource(), referenceId).orElse(transaction);
        }

        // Lock wallet and credit
        Wallet wallet = walletRepository.findByIdWithLock(transaction.getWallet().getId())
                .orElseThrow(() -> new WalletException("Wallet not found", WalletErrorCode.WALLET_NOT_FOUND));

        wallet.credit(transaction.getAmount());
        transaction.markSuccess();

        walletRepository.save(wallet);
        walletTransactionRepository.save(transaction);

        log.info("Confirmed pending transaction: referenceId={}, amount={}, newBalance={}",
                referenceId, transaction.getAmount(), wallet.getBalance());

        return transaction;
    }

    /**
     * Mark a pending transaction as failed.
     */
    @Transactional
    public WalletTransaction failPendingTransaction(String referenceId, String reason) {
        WalletTransaction transaction = walletTransactionRepository
                .findByReferenceIdAndStatus(referenceId, WalletTransactionStatus.PENDING)
                .orElse(null);

        if (transaction == null) {
            log.warn("Pending transaction not found for failure: referenceId={}", referenceId);
            return null;
        }

        transaction.markFailed();
        transaction.setDescription(transaction.getDescription() + " | Failed: " + reason);

        WalletTransaction saved = walletTransactionRepository.save(transaction);
        log.info("Marked transaction as failed: referenceId={}, reason={}", referenceId, reason);

        return saved;
    }

    // ==================== Transaction History ====================

    /**
     * Get paginated transaction history for a wallet.
     */
    @Transactional(readOnly = true)
    public Page<WalletTransactionDto> getTransactionHistory(Long userId, Pageable pageable) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletException("Wallet not found", WalletErrorCode.WALLET_NOT_FOUND));

        return walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable)
                .map(this::toTransactionDto);
    }

    // ==================== Admin Operations ====================

    /**
     * Block a wallet (admin operation).
     */
    @Transactional
    public void blockWallet(Long walletId, String reason) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletException("Wallet not found", WalletErrorCode.WALLET_NOT_FOUND));

        wallet.setStatus(WalletStatus.BLOCKED);
        walletRepository.save(wallet);
        log.info("Blocked wallet: walletId={}, reason={}", walletId, reason);
    }

    /**
     * Unblock a wallet (admin operation).
     */
    @Transactional
    public void unblockWallet(Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletException("Wallet not found", WalletErrorCode.WALLET_NOT_FOUND));

        wallet.setStatus(WalletStatus.ACTIVE);
        walletRepository.save(wallet);
        log.info("Unblocked wallet: walletId={}", walletId);
    }

    /**
     * Admin credit to wallet.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public WalletTransaction adminCredit(Long userId, BigDecimal amount, String reason, String adminId) {
        String referenceId = "ADMIN_CREDIT_" + adminId + "_" + System.currentTimeMillis();
        return creditWallet(userId, amount, WalletTransactionSource.ADMIN, referenceId,
                "Admin credit: " + reason);
    }

    /**
     * Admin debit from wallet.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public WalletTransaction adminDebit(Long userId, BigDecimal amount, String reason, String adminId) {
        String referenceId = "ADMIN_DEBIT_" + adminId + "_" + System.currentTimeMillis();
        return debitWallet(userId, amount, WalletTransactionSource.ADMIN, referenceId,
                "Admin debit: " + reason);
    }

    // ==================== Validation Helpers ====================

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new WalletException("Amount must be positive", WalletErrorCode.INVALID_AMOUNT);
        }
    }

    private void validateWalletActive(Wallet wallet) {
        if (!wallet.isActive()) {
            throw new WalletException("Wallet is blocked", WalletErrorCode.WALLET_BLOCKED);
        }
    }

    // ==================== DTO Converters ====================

    private WalletDto toDto(Wallet wallet) {
        return WalletDto.builder()
                .id(wallet.getId())
                .userId(wallet.getUser().getId())
                .balance(wallet.getBalance())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();
    }

    private WalletTransactionDto toTransactionDto(WalletTransaction tx) {
        return WalletTransactionDto.builder()
                .id(tx.getId())
                .walletId(tx.getWallet().getId())
                .amount(tx.getAmount())
                .type(tx.getType())
                .source(tx.getSource())
                .referenceId(tx.getReferenceId())
                .status(tx.getStatus())
                .description(tx.getDescription())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}

