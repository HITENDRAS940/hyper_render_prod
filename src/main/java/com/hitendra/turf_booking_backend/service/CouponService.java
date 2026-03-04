package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.coupon.CouponApplyResponseDto;
import com.hitendra.turf_booking_backend.dto.coupon.CouponDto;
import com.hitendra.turf_booking_backend.dto.coupon.CouponSummaryDto;
import com.hitendra.turf_booking_backend.dto.coupon.CreateCouponRequest;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.exception.BookingException;
import com.hitendra.turf_booking_backend.repository.BookingRepository;
import com.hitendra.turf_booking_backend.repository.CouponRepository;
import com.hitendra.turf_booking_backend.repository.CouponUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final BookingRepository bookingRepository;

    // ══════════════════════════════════════════════════════════════════════════
    // MANAGER — CRUD
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public CouponDto createCoupon(CreateCouponRequest request) {
        if (couponRepository.existsByCode(request.getCode().toUpperCase())) {
            throw new RuntimeException("Coupon code already exists: " + request.getCode().toUpperCase());
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minBookingAmount(request.getMinBookingAmount())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .validFrom(request.getValidFrom())
                .expiryDate(request.getExpiryDate())
                .usageLimit(request.getUsageLimit())
                .perUserUsageLimit(request.getPerUserUsageLimit() != null ? request.getPerUserUsageLimit() : 1)
                .newUsersOnly(request.isNewUsersOnly())
                .applicableServiceIds(request.getApplicableServiceIds() != null ? request.getApplicableServiceIds() : new java.util.HashSet<>())
                .applicableResourceIds(request.getApplicableResourceIds() != null ? request.getApplicableResourceIds() : new java.util.HashSet<>())
                .applicableActivityCodes(request.getApplicableActivityCodes() != null ? request.getApplicableActivityCodes() : new java.util.HashSet<>())
                .minBookingDurationMinutes(request.getMinBookingDurationMinutes())
                .validDayType(request.getValidDayType())
                .active(true)
                .build();

        Coupon saved = couponRepository.save(coupon);
        log.info("Coupon created: code={}, type={}, value={}", saved.getCode(), saved.getDiscountType(), saved.getDiscountValue());
        return convertToDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CouponSummaryDto> getAllCoupons() {
        return couponRepository.findAllSummaries()
                .stream()
                .map(p -> new CouponSummaryDto(p.getId(), p.getCode(), p.getDescription()))
                .collect(Collectors.toList());
    }

    /**
     * Public — returns only active, currently-valid coupons (code + description).
     * No auth required; safe to expose without any user context.
     */
    @Transactional(readOnly = true)
    public List<CouponSummaryDto> getAvailableCoupons() {
        return couponRepository.findAllAvailableSummaries(LocalDate.now())
                .stream()
                .map(p -> new CouponSummaryDto(p.getId(), p.getCode(), p.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CouponDto getCouponById(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + id));
        return convertToDto(coupon);
    }

    /** Soft-delete: marks the coupon inactive so it can no longer be applied. */
    @Transactional
    public void deactivateCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + id));
        coupon.setActive(false);
        couponRepository.save(coupon);
        log.info("Coupon deactivated: id={}, code={}", id, coupon.getCode());
    }

    /** Reactivate a previously deactivated coupon. */
    @Transactional
    public CouponDto activateCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Coupon not found: " + id));
        coupon.setActive(true);
        couponRepository.save(coupon);
        log.info("Coupon activated: id={}, code={}", id, coupon.getCode());
        return convertToDto(coupon);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // USER — APPLY COUPON
    // ══════════════════════════════════════════════════════════════════════════

    @Transactional
    public CouponApplyResponseDto applyCoupon(Long bookingId, String code, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found"));

        if (booking.getUser() == null || !booking.getUser().getId().equals(user.getId())) {
            throw new BookingException("Access denied: You can only apply coupons to your own bookings");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingException("Coupons can only be applied to PENDING bookings");
        }

        if (booking.getAppliedCouponCode() != null) {
            throw new BookingException("A coupon has already been applied to this booking");
        }

        Coupon coupon = couponRepository.findByCodeAndActiveTrue(code.toUpperCase())
                .orElseThrow(() -> new BookingException("Invalid or inactive coupon code"));

        // ── Run all constraint checks ─────────────────────────────────────────
        validateCouponEligibility(coupon, user, booking);

        // ── Calculate discount ────────────────────────────────────────────────
        BigDecimal totalAmount = BigDecimal.valueOf(booking.getAmount());
        double originalAmount = booking.getAmount();                         // capture before mutation
        BigDecimal discount = calculateDiscount(coupon, totalAmount);
        BigDecimal newTotal = totalAmount.subtract(discount).setScale(2, RoundingMode.HALF_UP);

        // ── Recalculate online / venue split ──────────────────────────────────
        double onlinePct = (booking.getService() != null && booking.getService().getOnlinePaymentPercent() != null)
                ? booking.getService().getOnlinePaymentPercent()
                : 20.0;
        BigDecimal onlineAmountPaid = newTotal
                .multiply(BigDecimal.valueOf(onlinePct))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal venueAmountDue = newTotal.subtract(onlineAmountPaid);

        // ── Persist all revised amounts to bookings table ─────────────────────
        booking.setDiscountAmount(discount);
        booking.setAppliedCouponCode(coupon.getCode());
        booking.setAmount(newTotal.doubleValue());
        booking.setOnlineAmountPaid(onlineAmountPaid);
        booking.setVenueAmountDue(venueAmountDue);
        bookingRepository.save(booking);

        // ── NOTE: CouponUsage is NOT recorded here. ───────────────────────────
        // The coupon is only marked as used (CouponUsage row created + currentUsage
        // incremented) when the booking is CONFIRMED via the Razorpay webhook
        // (payment.captured event). If payment never completes, the coupon remains
        // available for the user to use on a future booking.

        log.info("Coupon {} applied to booking {}. Original: {}, Discount: {}, New total: {}",
                code, bookingId, totalAmount, discount, newTotal);

        return buildDiscountResponse(booking, originalAmount, discount, onlinePct);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // VALIDATION ENGINE
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Runs every constraint defined on the coupon against the current booking context.
     * Throws {@link BookingException} with a human-friendly message on the first failure.
     */
    private void validateCouponEligibility(Coupon coupon, User user, Booking booking) {
        LocalDate today = LocalDate.now();

        // 1. Active window (validFrom – expiryDate)
        if (coupon.getValidFrom() != null && today.isBefore(coupon.getValidFrom())) {
            throw new BookingException("This coupon is not active yet. It becomes valid on " + coupon.getValidFrom());
        }
        if (coupon.getExpiryDate().isBefore(today)) {
            throw new BookingException("This coupon has expired");
        }

        // 2. Global usage cap
        if (coupon.getUsageLimit() != null && coupon.getCurrentUsage() >= coupon.getUsageLimit()) {
            throw new BookingException("This coupon has reached its maximum usage limit");
        }

        // 3. Per-user usage cap
        int perUserLimit = coupon.getPerUserUsageLimit() != null ? coupon.getPerUserUsageLimit() : 1;
        long userUsageCount = couponUsageRepository.countByUserIdAndCouponId(user.getId(), coupon.getId());
        if (userUsageCount >= perUserLimit) {
            throw new BookingException(perUserLimit == 1
                    ? "You have already used this coupon"
                    : "You have reached the maximum usage limit (" + perUserLimit + "x) for this coupon");
        }

        // 4. New-users-only
        if (coupon.isNewUsersOnly()) {
            long completedBookings = bookingRepository.countCompletedBookingsByUserId(user.getId());
            if (completedBookings > 0) {
                throw new BookingException("This coupon is only valid for users making their first booking");
            }
        }

        // 5. Minimum booking amount
        if (coupon.getMinBookingAmount() != null && booking.getAmount() < coupon.getMinBookingAmount()) {
            throw new BookingException(
                    "This coupon requires a minimum booking amount of ₹" + coupon.getMinBookingAmount());
        }

        // 6. Service scope
        if (!coupon.getApplicableServiceIds().isEmpty()) {
            Long serviceId = booking.getService() != null ? booking.getService().getId() : null;
            if (serviceId == null || !coupon.getApplicableServiceIds().contains(serviceId)) {
                throw new BookingException("This coupon is not valid for the selected service");
            }
        }

        // 7. Resource scope
        if (!coupon.getApplicableResourceIds().isEmpty()) {
            Long resourceId = booking.getResource() != null ? booking.getResource().getId() : null;
            if (resourceId == null || !coupon.getApplicableResourceIds().contains(resourceId)) {
                throw new BookingException("This coupon is not valid for the selected resource");
            }
        }

        // 8. Activity scope
        if (!coupon.getApplicableActivityCodes().isEmpty()) {
            String activityCode = booking.getActivityCode();
            if (activityCode == null || !coupon.getApplicableActivityCodes().contains(activityCode.toUpperCase())) {
                throw new BookingException("This coupon is not valid for the selected activity");
            }
        }

        // 9. Minimum booking duration
        if (coupon.getMinBookingDurationMinutes() != null) {
            long durationMinutes = Duration.between(booking.getStartTime(), booking.getEndTime()).toMinutes();
            if (durationMinutes < coupon.getMinBookingDurationMinutes()) {
                throw new BookingException(
                        "This coupon requires a minimum booking duration of "
                        + coupon.getMinBookingDurationMinutes() + " minutes");
            }
        }

        // 10. Day-type restriction (WEEKDAY / WEEKEND)
        if (coupon.getValidDayType() != null && coupon.getValidDayType() != DayType.ALL) {
            DayOfWeek dow = booking.getBookingDate().getDayOfWeek();
            boolean isWeekend = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
            if (coupon.getValidDayType() == DayType.WEEKEND && !isWeekend) {
                throw new BookingException("This coupon is only valid for weekend bookings");
            }
            if (coupon.getValidDayType() == DayType.WEEKDAY && isWeekend) {
                throw new BookingException("This coupon is only valid for weekday bookings");
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DISCOUNT CALCULATION
    // ══════════════════════════════════════════════════════════════════════════

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal amount) {
        BigDecimal discount;

        if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = amount.multiply(BigDecimal.valueOf(coupon.getDiscountValue()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            // Apply cap for percentage discounts
            if (coupon.getMaxDiscountAmount() != null) {
                BigDecimal cap = BigDecimal.valueOf(coupon.getMaxDiscountAmount());
                if (discount.compareTo(cap) > 0) {
                    discount = cap;
                }
            }
        } else {
            // FIXED
            discount = BigDecimal.valueOf(coupon.getDiscountValue());
        }

        // Discount can never exceed the booking total
        if (discount.compareTo(amount) > 0) {
            discount = amount;
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CONVERTERS
    // ══════════════════════════════════════════════════════════════════════════

    private CouponDto convertToDto(Coupon coupon) {
        return CouponDto.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minBookingAmount(coupon.getMinBookingAmount())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .validFrom(coupon.getValidFrom())
                .expiryDate(coupon.getExpiryDate())
                .active(coupon.isActive())
                .usageLimit(coupon.getUsageLimit())
                .currentUsage(coupon.getCurrentUsage())
                .perUserUsageLimit(coupon.getPerUserUsageLimit())
                .newUsersOnly(coupon.isNewUsersOnly())
                .applicableServiceIds(coupon.getApplicableServiceIds())
                .applicableResourceIds(coupon.getApplicableResourceIds())
                .applicableActivityCodes(coupon.getApplicableActivityCodes())
                .minBookingDurationMinutes(coupon.getMinBookingDurationMinutes())
                .validDayType(coupon.getValidDayType())
                .createdAt(coupon.getCreatedAt())
                .build();
    }

    private CouponApplyResponseDto buildDiscountResponse(Booking booking, double originalAmount,
                                                          BigDecimal discount, double onlinePct) {
        return CouponApplyResponseDto.builder()
                .bookingId(booking.getId())
                .couponCode(booking.getAppliedCouponCode())
                .originalAmount(originalAmount)
                .discountAmount(discount.doubleValue())
                .revisedTotal(booking.getAmount())
                .onlineAmount(booking.getOnlineAmountPaid() != null
                        ? booking.getOnlineAmountPaid().doubleValue() : 0.0)
                .venueAmount(booking.getVenueAmountDue() != null
                        ? booking.getVenueAmountDue().doubleValue() : 0.0)
                .onlinePaymentPercent(onlinePct)
                .currency("INR")
                .build();
    }
}
