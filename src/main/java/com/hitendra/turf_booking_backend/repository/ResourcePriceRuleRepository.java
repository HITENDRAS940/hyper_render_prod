package com.hitendra.turf_booking_backend.repository;

import com.hitendra.turf_booking_backend.entity.DayType;
import com.hitendra.turf_booking_backend.entity.ResourcePriceRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ResourcePriceRuleRepository extends JpaRepository<ResourcePriceRule, Long> {

    List<ResourcePriceRule> findByResourceSlotConfig_Resource_IdAndEnabledTrueOrderByPriorityDesc(Long resourceId);

    List<ResourcePriceRule> findByResourceSlotConfig_Resource_IdOrderByPriorityDesc(Long resourceId);

    /**
     * Find applicable rules for a resource, day type, and time
     * Returns rules where the slot time falls within the rule's time range
     * Ordered by priority (highest first)
     */
    @Query("SELECT r FROM ResourcePriceRule r WHERE r.resourceSlotConfig.resource.id = :resourceId " +
           "AND r.enabled = true " +
           "AND (r.dayType = :dayType OR r.dayType = 'ALL') " +
           "AND r.startTime <= :slotTime AND r.endTime > :slotTime " +
           "ORDER BY r.priority DESC")
    List<ResourcePriceRule> findApplicableRules(
            @Param("resourceId") Long resourceId,
            @Param("dayType") DayType dayType,
            @Param("slotTime") LocalTime slotTime);

    /**
     * Find all enabled rules for a resource
     */
    @Query("SELECT r FROM ResourcePriceRule r WHERE r.resourceSlotConfig.resource.id = :resourceId " +
           "AND r.enabled = true ORDER BY r.priority DESC, r.startTime ASC")
    List<ResourcePriceRule> findEnabledRulesByResourceId(@Param("resourceId") Long resourceId);

    void deleteByResourceSlotConfig_Resource_Id(Long resourceId);

    // ==================== OPTIMIZED QUERIES ====================

    /**
     * Get only the base price for a specific slot (minimal data for quick pricing).
     * Returns the first applicable rule's base price (highest priority).
     * Note: extraCharge should be added separately if needed.
     * NOTE: Caller should use .stream().findFirst() to get single result
     */
    @Query("""
        SELECT r.basePrice FROM ResourcePriceRule r 
        WHERE r.resourceSlotConfig.resource.id = :resourceId 
        AND r.enabled = true 
        AND (r.dayType = :dayType OR r.dayType = 'ALL') 
        AND r.startTime <= :slotTime AND r.endTime > :slotTime 
        ORDER BY r.priority DESC
        """)
    List<Double> findApplicableBasePriceList(
            @Param("resourceId") Long resourceId,
            @Param("dayType") DayType dayType,
            @Param("slotTime") LocalTime slotTime);

    /**
     * Get base price and extra charge for a specific slot.
     * Returns [basePrice, extraCharge] for the highest priority applicable rule.
     * NOTE: Caller should use .stream().findFirst() to get single result
     */
    @Query("""
        SELECT r.basePrice, r.extraCharge FROM ResourcePriceRule r 
        WHERE r.resourceSlotConfig.resource.id = :resourceId 
        AND r.enabled = true 
        AND (r.dayType = :dayType OR r.dayType = 'ALL') 
        AND r.startTime <= :slotTime AND r.endTime > :slotTime 
        ORDER BY r.priority DESC
        """)
    List<Object[]> findApplicablePriceComponentsList(
            @Param("resourceId") Long resourceId,
            @Param("dayType") DayType dayType,
            @Param("slotTime") LocalTime slotTime);

    /**
     * Check if any price rule exists for resource (faster than loading rules).
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM ResourcePriceRule r WHERE r.resourceSlotConfig.resource.id = :resourceId AND r.enabled = true")
    boolean existsEnabledRulesForResource(@Param("resourceId") Long resourceId);

    /**
     * Get count of enabled rules for a resource.
     */
    @Query("SELECT COUNT(r) FROM ResourcePriceRule r WHERE r.resourceSlotConfig.resource.id = :resourceId AND r.enabled = true")
    long countEnabledRulesByResourceId(@Param("resourceId") Long resourceId);

    // ==================== END OPTIMIZED QUERIES ====================
}

