package com.hitendra.turf_booking_backend.service.accounting;

import com.hitendra.turf_booking_backend.dto.accounting.CreateExpenseCategoryRequest;
import com.hitendra.turf_booking_backend.entity.accounting.ExpenseCategory;
import com.hitendra.turf_booking_backend.entity.accounting.ExpenseType;
import com.hitendra.turf_booking_backend.exception.BookingException;
import com.hitendra.turf_booking_backend.repository.accounting.ExpenseCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing expense categories (master data).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseCategoryService {

    private final ExpenseCategoryRepository categoryRepository;

    /**
     * Create a new expense category.
     */
    @Transactional
    public ExpenseCategory createCategory(CreateExpenseCategoryRequest request) {
        log.info("Creating expense category: {}", request.getName());

        if (categoryRepository.existsByName(request.getName())) {
            throw new BookingException("Expense category already exists: " + request.getName());
        }

        ExpenseCategory category = ExpenseCategory.builder()
            .name(request.getName())
            .type(request.getType())
            .build();

        ExpenseCategory saved = categoryRepository.save(category);
        log.info("Expense category created: ID={}", saved.getId());

        return saved;
    }

    /**
     * Get all expense categories.
     */
    public List<ExpenseCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    /**
     * Get categories by type.
     */
    public List<ExpenseCategory> getCategoriesByType(ExpenseType type) {
        return categoryRepository.findByType(type);
    }

    /**
     * Get category by ID.
     */
    public ExpenseCategory getCategoryById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new BookingException("Expense category not found: " + id));
    }

    /**
     * Get category by name.
     */
    public ExpenseCategory getCategoryByName(String name) {
        return categoryRepository.findByName(name)
            .orElseThrow(() -> new BookingException("Expense category not found: " + name));
    }
}

