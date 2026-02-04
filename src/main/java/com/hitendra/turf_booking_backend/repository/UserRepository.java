package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.User;
import com.hitendra.turf_booking_backend.entity.Role;
import com.hitendra.turf_booking_backend.repository.projection.UserBasicProjection;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhone(String phone);

    User findUserByPhone(String phone);

    Optional<User> findByEmail(String email);

    Optional<User> findUserByEmail(String email);

    Optional<User> findByOauthProviderIdAndOauthProvider(String oauthProviderId, com.hitendra.turf_booking_backend.entity.OAuthProvider oauthProvider);

    /**
     * Find all users with a specific role (paginated)
     */
    Page<User> findByRole(Role role, Pageable pageable);

    /**
     * Find all users excluding admins and managers (paginated)
     */
    @Query("SELECT u FROM User u WHERE u.role = 'USER'")
    Page<User> findAllRegularUsers(Pageable pageable);

    // ==================== OPTIMIZED PROJECTION QUERIES ====================

    /**
     * Get lightweight user list (projection-based).
     * Only fetches essential fields for list views.
     */
    @Query("""
        SELECT u.id as id, u.phone as phone, u.email as email, u.name as name,
               CAST(u.role AS string) as role, u.enabled as enabled, u.createdAt as createdAt
        FROM User u
        WHERE u.role = 'USER'
        ORDER BY u.createdAt DESC
        """)
    Page<UserBasicProjection> findAllRegularUsersProjected(Pageable pageable);

    /**
     * Get lightweight user by ID (projection-based).
     */
    @Query("""
        SELECT u.id as id, u.phone as phone, u.email as email, u.name as name,
               CAST(u.role AS string) as role, u.enabled as enabled, u.createdAt as createdAt
        FROM User u
        WHERE u.id = :userId AND u.role = 'USER'
        """)
    Optional<UserBasicProjection> findUserByIdProjected(@Param("userId") Long userId);

    /**
     * Check if user exists by phone (faster than fetching full entity).
     */
    boolean existsByPhone(String phone);

    /**
     * Check if user exists by email (faster than fetching full entity).
     */
    boolean existsByEmail(String email);

    /**
     * Get user ID by phone (minimal data fetch).
     */
    @Query("SELECT u.id FROM User u WHERE u.phone = :phone")
    Optional<Long> findUserIdByPhone(@Param("phone") String phone);

    /**
     * Get user ID by email (minimal data fetch).
     */
    @Query("SELECT u.id FROM User u WHERE u.email = :email")
    Optional<Long> findUserIdByEmail(@Param("email") String email);

    /**
     * Get user role by ID (minimal data fetch for authorization).
     */
    @Query("SELECT u.role FROM User u WHERE u.id = :userId")
    Optional<Role> findRoleByUserId(@Param("userId") Long userId);

    // ==================== END OPTIMIZED PROJECTION QUERIES ====================
}
