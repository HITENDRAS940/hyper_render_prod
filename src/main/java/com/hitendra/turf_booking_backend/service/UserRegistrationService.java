package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.entity.User;
import com.hitendra.turf_booking_backend.entity.Wallet;
import com.hitendra.turf_booking_backend.entity.WalletStatus;
import com.hitendra.turf_booking_backend.repository.UserRepository;
import com.hitendra.turf_booking_backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Service responsible for user registration with wallet creation.
 * Ensures atomic creation of user and wallet in a single transaction.
 *
 * IMPORTANT: This is the ONLY place where wallets should be created.
 * Wallet creation must happen during user registration, not on login or any other flow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    /**
     * Registers a new user with their wallet atomically.
     * This method:
     * 1. Creates and persists the user
     * 2. Creates a wallet with balance = 0 and status = ACTIVE
     *
     * Both operations happen in a single transaction.
     * If either fails, the entire transaction rolls back.
     *
     * @param phone The phone number of the user
     * @return The created User entity
     * @throws IllegalStateException if user or wallet already exists (for safety)
     */
    @Transactional
    public User registerNewUserWithWallet(String phone) {
        // Safety check: Ensure no user exists with this phone
        if (userRepository.findByPhone(phone).isPresent()) {
            throw new IllegalStateException("User already exists with phone: " + phone);
        }

        // Generate email from phone (temporary email for phone-only users)
        // Format: phone_<phoneNumber>@temp.hyper.com
        String tempEmail = "phone_" + phone.replaceAll("[^0-9]", "") + "@temp.hyper.com";

        // Create and persist the user first
        User user = User.builder()
                .phone(phone)
                .email(tempEmail)
                .build();
        User savedUser = userRepository.save(user);

        log.info("Created new user: userId={}, phone={}, email={}", savedUser.getId(), phone, tempEmail);

        // Create wallet for the user - this happens in the same transaction
        // The unique constraint on wallet.user_id prevents duplicate wallets
        Wallet wallet = Wallet.builder()
                .user(savedUser)
                .balance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Created wallet for user: userId={}, walletId={}", savedUser.getId(), savedWallet.getId());

        return savedUser;
    }

    /**
     * Check if a user exists by phone number.
     * This is a read-only operation.
     *
     * @param phone The phone number to check
     * @return true if user exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean userExists(String phone) {
        return userRepository.findByPhone(phone).isPresent();
    }

    /**
     * Create wallet for an existing user (used for OAuth users)
     * This should only be called for users created through OAuth flow
     *
     * @param user The user entity
     * @return The created Wallet entity
     */
    @Transactional
    public Wallet createWalletForUser(User user) {
        // Check if wallet already exists
        if (user.getWallet() != null) {
            log.warn("Wallet already exists for user: {}", user.getId());
            return user.getWallet();
        }

        Wallet wallet = Wallet.builder()
                .user(user)
                .balance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Created wallet for OAuth user: userId={}, walletId={}", user.getId(), savedWallet.getId());

        return savedWallet;
    }
}

