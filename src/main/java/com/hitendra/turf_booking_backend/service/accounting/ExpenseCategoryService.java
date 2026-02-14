package com.hitendra.turf_booking_backend.service.accounting;

import com.hitendra.turf_booking_backend.dto.accounting.CreateExpenseCategoryRequest;
import com.hitendra.turf_booking_backend.dto.user.AdminProfileDto;
import com.hitendra.turf_booking_backend.entity.AdminProfile;
import com.hitendra.turf_booking_backend.entity.accounting.ExpenseCategory;
import com.hitendra.turf_booking_backend.entity.accounting.ExpenseType;
import com.hitendra.turf_booking_backend.exception.BookingException;
import com.hitendra.turf_booking_backend.repository.AdminProfileRepository;
import com.hitendra.turf_booking_backend.repository.accounting.ExpenseCategoryRepository;
import com.hitendra.turf_booking_backend.service.AdminProfileService;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing admin-specific expense categories.
 * Each admin has their own set of expense categories.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository categoryRepository;
    private final AuthUtil authUtil;
    private final AdminProfileRepository adminProfileRepository;

    /**
     * Create a new expense category for current admin.
     */
    @Transactional
    public ExpenseCategory createCategory(CreateExpenseCategoryRequest request) {
        // Get current admin
        AdminProfile currentAdmin = authUtil.getCurrentAdminProfile();

        log.info("Creating expense category for admin {}: {}", currentAdmin.getId(), request.getName());

        // Check if category with same name already exists for this admin
        if (categoryRepository.existsByAdminProfileIdAndName(currentAdmin.getId(), request.getName())) {
            throw new BookingException("You already have an expense category named: " + request.getName());
        }

        ExpenseCategory category = ExpenseCategory.builder()
            .adminProfile(currentAdmin)
            .name(request.getName())
            .type(request.getType())
            .build();

        ExpenseCategory saved = categoryRepository.save(category);
        log.info("Expense category created: ID={} for admin {}", saved.getId(), currentAdmin.getId());

        return saved;
    }

    /**
     * Get all expense categories for current admin.
     */
    public List<ExpenseCategory> getAllCategories() {
        AdminProfile currentAdmin = authUtil.getCurrentAdminProfile();
        return categoryRepository.findByAdminProfileId(currentAdmin.getId());
    }

    /**
     * Get categories by type for current admin.
     */
    public List<ExpenseCategory> getCategoriesByType(ExpenseType type) {
        AdminProfile currentAdmin = authUtil.getCurrentAdminProfile();
        return categoryRepository.findByAdminProfileIdAndType(currentAdmin.getId(), type);
    }

    /**
     * Get category by ID.
     * Ensures the category belongs to current admin.
     */
    public ExpenseCategory getCategoryById(Long id) {
        AdminProfile currentAdmin = authUtil.getCurrentAdminProfile();

        ExpenseCategory category = categoryRepository.findById(id)
            .orElseThrow(() -> new BookingException("Expense category not found: " + id));

        // Verify ownership
        if (!category.getAdminProfile().getId().equals(currentAdmin.getId())) {
            throw new BookingException("Access denied: This category belongs to another admin");
        }

        return category;
    }

    /**
     * Get category by name for current admin.
     */
    public ExpenseCategory getCategoryByName(String name) {
        AdminProfile currentAdmin = authUtil.getCurrentAdminProfile();

        return categoryRepository.findByAdminProfileIdAndName(currentAdmin.getId(), name)
            .orElseThrow(() -> new BookingException("Expense category not found: " + name));
    }
}

