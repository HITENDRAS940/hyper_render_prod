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

    Optional<ExpenseCategory> findByName(String name);

    List<ExpenseCategory> findByType(ExpenseType type);

    boolean existsByName(String name);

    // ==================== OPTIMIZED QUERIES ====================

    /**
     * Get category ID by name (minimal data fetch).
     */
    @Query("SELECT ec.id FROM ExpenseCategory ec WHERE ec.name = :name")
    Optional<Long> findCategoryIdByName(@Param("name") String name);

    /**
     * Get only category names (for dropdown/autocomplete).
     */
    @Query("SELECT ec.name FROM ExpenseCategory ec ORDER BY ec.name")
    List<String> findAllCategoryNames();

    /**
     * Get category names by type (for filtered dropdown).
     */
    @Query("SELECT ec.name FROM ExpenseCategory ec WHERE ec.type = :type ORDER BY ec.name")
    List<String> findCategoryNamesByType(@Param("type") ExpenseType type);

    // ==================== END OPTIMIZED QUERIES ====================
}

