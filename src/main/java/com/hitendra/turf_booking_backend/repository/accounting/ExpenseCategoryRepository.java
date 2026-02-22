package com.hitendra.turf_booking_backend.repository.accounting;

import com.hitendra.turf_booking_backend.entity.accounting.ExpenseCategory;
import com.hitendra.turf_booking_backend.entity.accounting.ExpenseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    // ==================== ADMIN-SPECIFIC QUERIES ====================

    /**
     * Get all categories for a specific admin.
     * Uses JOIN FETCH to avoid LazyInitializationException when open-in-view is disabled.
     */
    @Query("SELECT ec FROM ExpenseCategory ec JOIN FETCH ec.adminProfile WHERE ec.adminProfile.id = :adminProfileId")
    List<ExpenseCategory> findByAdminProfileId(@Param("adminProfileId") Long adminProfileId);

    /**
     * Get categories by admin and type.
     * Uses JOIN FETCH to avoid LazyInitializationException when open-in-view is disabled.
     */
    @Query("SELECT ec FROM ExpenseCategory ec JOIN FETCH ec.adminProfile WHERE ec.adminProfile.id = :adminProfileId AND ec.type = :type")
    List<ExpenseCategory> findByAdminProfileIdAndType(@Param("adminProfileId") Long adminProfileId, @Param("type") ExpenseType type);

    /**
     * Find category by admin and name.
     * Uses JOIN FETCH to avoid LazyInitializationException when open-in-view is disabled.
     */
    @Query("SELECT ec FROM ExpenseCategory ec JOIN FETCH ec.adminProfile WHERE ec.adminProfile.id = :adminProfileId AND ec.name = :name")
    Optional<ExpenseCategory> findByAdminProfileIdAndName(@Param("adminProfileId") Long adminProfileId, @Param("name") String name);

    /**
     * Check if category exists for admin with given name.
     */
    boolean existsByAdminProfileIdAndName(Long adminProfileId, String name);

    /**
     * Find category by ID with adminProfile eagerly loaded.
     * Use this instead of findById to avoid LazyInitializationException when open-in-view is disabled.
     */
    @Query("SELECT ec FROM ExpenseCategory ec JOIN FETCH ec.adminProfile WHERE ec.id = :id")
    Optional<ExpenseCategory> findByIdWithAdminProfile(@Param("id") Long id);

    // ==================== LEGACY METHODS (Deprecated - use admin-specific versions) ====================

    @Deprecated
    Optional<ExpenseCategory> findByName(String name);

    @Deprecated
    List<ExpenseCategory> findByType(ExpenseType type);

    @Deprecated
    boolean existsByName(String name);

    // ==================== OPTIMIZED QUERIES ====================

    /**
     * Get category ID by admin and name (minimal data fetch).
     */
    @Query("SELECT ec.id FROM ExpenseCategory ec WHERE ec.adminProfile.id = :adminProfileId AND ec.name = :name")
    Optional<Long> findCategoryIdByAdminAndName(@Param("adminProfileId") Long adminProfileId, @Param("name") String name);

    /**
     * Get only category names for an admin (for dropdown/autocomplete).
     */
    @Query("SELECT ec.name FROM ExpenseCategory ec WHERE ec.adminProfile.id = :adminProfileId ORDER BY ec.name")
    List<String> findCategoryNamesByAdmin(@Param("adminProfileId") Long adminProfileId);

    /**
     * Get category names by admin and type (for filtered dropdown).
     */
    @Query("SELECT ec.name FROM ExpenseCategory ec WHERE ec.adminProfile.id = :adminProfileId AND ec.type = :type ORDER BY ec.name")
    List<String> findCategoryNamesByAdminAndType(@Param("adminProfileId") Long adminProfileId, @Param("type") ExpenseType type);

    // ==================== END OPTIMIZED QUERIES ====================
}

