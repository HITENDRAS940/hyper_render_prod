package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.booking.*;
import com.hitendra.turf_booking_backend.dto.common.PaginatedResponse;
import com.hitendra.turf_booking_backend.entity.*;
import com.hitendra.turf_booking_backend.repository.*;
import com.hitendra.turf_booking_backend.util.AuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceResourceRepository serviceResourceRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;
    private final EmailService emailService;
    private final ResourceSlotService resourceSlotService;
    private final PricingService pricingService;
    private final AuthUtil authUtil;

    @Value("${pricing.online-payment-percent:20}")
    private Double onlinePaymentPercent;

    // Auto-confirm is now handled via Razorpay webhook only

    /**
     * Create a booking for a user
     */
    public BookingResponseDto createUserBooking(BookingRequestDto request) {

        // Fetch resource
        ServiceResource resource = serviceResourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        // Validate time range
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new RuntimeException("Start time must be before end time");
        }

        // Check if resource is enabled
        if (!resource.isEnabled()) {
            throw new RuntimeException("Resource is not available for booking");
        }

        Service service = resource.getService();

        // Don't allow booking for past dates
        if (request.getBookingDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot book slots for past dates");
        }

        User user = authUtil.getCurrentUser();

        // Check for overlapping bookings
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(
                request.getResourceId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (!overlappingBookings.isEmpty()) {
            throw new RuntimeException("Selected time range overlaps with an existing booking");
        }

        // Calculate total amount with taxes and fees using PricingService
        PriceBreakdownDto priceBreakdown = pricingService.calculatePriceBreakdownForTimeRange(
                request.getResourceId(),
                request.getStartTime(),
                request.getEndTime(),
                request.getBookingDate()
        );
        double totalAmount = priceBreakdown.getTotalAmount();

        String reference = generateBookingReference();

        // Create booking
        Booking booking = Booking.builder()
                .user(user)
                .service(service)
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .bookingDate(request.getBookingDate())
                .amount(totalAmount)
                .reference(reference)
                .status(BookingStatus.PENDING)
                .createdAt(java.time.Instant.now())
                .paymentSource(PaymentSource.BY_USER)
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("Created booking {} for user {} from {} to {}, total: {} (subtotal: {}, tax: {}, fee: {})",
                reference, user.getPhone(), request.getStartTime(), request.getEndTime(), totalAmount,
                priceBreakdown.getSubtotal(),  priceBreakdown.getConvenienceFee());

        return convertToResponseDto(saved);
    }

    /**
     * Confirm a booking after payment
     */
    public void confirmBooking(String reference) {
        Booking booking = findBookingByReference(reference);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);


        log.info("Confirmed booking {}", reference);
        sendBookingNotifications(booking);
    }

    /**
     * Cancel a booking by reference
     */
    public void cancelBooking(String reference) {
        Booking booking = findBookingByReference(reference);


        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Cancelled booking {}", reference);
    }

    /**
     * Cancel a booking by ID (admin)
     */
    public void cancelBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking is already cancelled");
        }


        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Admin cancelled booking {}", bookingId);
    }

    /**
     * Get booking by reference
     */
    public BookingResponseDto getBookingByReference(String reference) {
        Booking booking = bookingRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Booking not found with reference: " + reference));
        return convertToResponseDto(booking);
    }

    /**
     * Get booking by ID
     */
    public BookingResponseDto getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));
        return convertToResponseDto(booking);
    }

    /**
     * Get all bookings
     */
    public PaginatedResponse<BookingResponseDto> getAllBookings(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Booking> bookingPage = bookingRepository.findAll(pageable);

        List<BookingResponseDto> content = bookingPage.getContent().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isLast()
        );
    }

    /**
     * Get bookings by service (optimized with projection)
     */
    public PaginatedResponse<BookingResponseDto> getBookingsByService(Long serviceId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<com.hitendra.turf_booking_backend.repository.projection.BookingListProjection> bookingPage =
                bookingRepository.findBookingsByServiceIdProjected(serviceId, pageable);

        List<BookingResponseDto> content = bookingPage.getContent().stream()
                .map(this::convertProjectionToResponseDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isLast()
        );
    }

    /**
     * Get bookings by status (optimized with projection)
     */
    public PaginatedResponse<BookingResponseDto> getBookingsByStatus(BookingStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<com.hitendra.turf_booking_backend.repository.projection.BookingListProjection> bookingPage =
                bookingRepository.findBookingsByStatusProjected(status, pageable);

        List<BookingResponseDto> content = bookingPage.getContent().stream()
                .map(this::convertProjectionToResponseDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isLast()
        );
    }

    /**
     * Get bookings by resource ID (optimized with projection)
     */
    public PaginatedResponse<BookingResponseDto> getBookingsByResource(Long resourceId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<com.hitendra.turf_booking_backend.repository.projection.BookingListProjection> bookingPage =
                bookingRepository.findBookingsByResourceIdProjected(resourceId, pageable);

        List<BookingResponseDto> content = bookingPage.getContent().stream()
                .map(this::convertProjectionToResponseDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isLast()
        );
    }

    /**
     * Get bookings by resource ID and optional date (optimized with projection)
     */
    public PaginatedResponse<BookingResponseDto> getBookingsByResourceAndDate(Long resourceId, LocalDate date, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<com.hitendra.turf_booking_backend.repository.projection.BookingListProjection> bookingPage;

        if (date != null) {
            bookingPage = bookingRepository.findBookingsByResourceIdAndDateProjected(resourceId, date, pageable);
        } else {
            bookingPage = bookingRepository.findBookingsByResourceIdProjected(resourceId, pageable);
        }

        List<BookingResponseDto> content = bookingPage.getContent().stream()
                .map(this::convertProjectionToResponseDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isLast()
        );
    }

    /**
     * Get bookings by user ID
     */
    public PaginatedResponse<BookingResponseDto> getBookingsByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Booking> bookingPage = bookingRepository.findByUserId(userId, pageable);

        List<BookingResponseDto> content = bookingPage.getContent().stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isLast()
        );
    }

    /**
     * Get bookings by admin ID (optimized with projection - for services created by this admin)
     */
    public PaginatedResponse<BookingResponseDto> getBookingsByAdminId(Long adminId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<com.hitendra.turf_booking_backend.repository.projection.BookingListProjection> bookingPage =
                bookingRepository.findBookingsByAdminIdProjected(adminId, pageable);

        List<BookingResponseDto> content = bookingPage.getContent().stream()
                .map(this::convertProjectionToResponseDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isLast()
        );
    }

    /**
     * Get bookings by admin ID with optional date and status filters (optimized with projection)
     * If date or status is null, that filter is not applied
     */
    public PaginatedResponse<BookingResponseDto> getBookingsByAdminIdWithFilters(
            Long adminId, java.time.LocalDate date, BookingStatus status, int page, int size) {
        log.info("Fetching bookings for adminId: {}, date: {}, status: {}, page: {}, size: {}",
                adminId, date, status, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<com.hitendra.turf_booking_backend.repository.projection.BookingListProjection> bookingPage;
        if (date != null && status != null) {
            log.info("Using query: findBookingsByAdminIdAndDateAndStatusProjected");
            bookingPage = bookingRepository.findBookingsByAdminIdAndDateAndStatusProjected(adminId, date, status, pageable);
        } else if (date != null) {
            log.info("Using query: findBookingsByAdminIdAndDateProjected");
            bookingPage = bookingRepository.findBookingsByAdminIdAndDateProjected(adminId, date, pageable);
        } else if (status != null) {
            log.info("Using query: findBookingsByAdminIdAndStatusProjected");
            bookingPage = bookingRepository.findBookingsByAdminIdAndStatusProjected(adminId, status, pageable);
        } else {
            log.info("Using query: findBookingsByAdminIdProjected (no filters)");
            bookingPage = bookingRepository.findBookingsByAdminIdProjected(adminId, pageable);
        }

        log.info("Query executed - Found {} total bookings for adminId: {}",
                bookingPage.getTotalElements(), adminId);
        log.info("Current page has {} bookings (page {}/{})",
                bookingPage.getContent().size(),
                bookingPage.getNumber() + 1,
                bookingPage.getTotalPages());

        // Log each booking found
        for (com.hitendra.turf_booking_backend.repository.projection.BookingListProjection projection : bookingPage.getContent()) {
            log.info("  - Booking ID: {}, Ref: {}, Status: {}, Date: {}",
                    projection.getId(),
                    projection.getReference(),
                    projection.getStatus(),
                    projection.getBookingDate());
        }

        log.info("Converting {} projections to DTOs...", bookingPage.getContent().size());
        List<BookingResponseDto> content = bookingPage.getContent().stream()
                .map(projection -> {
                    BookingResponseDto dto = convertProjectionToResponseDto(projection);
                    log.info("  - Converted Booking ID: {} to DTO", dto.getId());
                    return dto;
                })
                .collect(Collectors.toList());

        log.info("Successfully converted {} bookings to DTOs", content.size());

        return new PaginatedResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isLast()
        );
    }

    /**
     * Get pending bookings by admin ID (for services created by this admin)
     */
    public PaginatedResponse<PendingBookingDto> getPendingBookingsByAdminId(Long adminId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Booking> bookingPage = bookingRepository.findPendingByServiceCreatedById(adminId, pageable);

        List<PendingBookingDto> content = bookingPage.getContent().stream()
                .map(this::convertToPendingBookingDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isLast()
        );
    }

    /**
     * Get current user's bookings (optimized with projection query)
     */
    public List<UserBookingDto> getCurrentUserBookings() {
        User currentUser = authUtil.getCurrentUser();
        List<com.hitendra.turf_booking_backend.repository.projection.UserBookingProjection> projections =
                bookingRepository.findUserBookingsProjected(currentUser.getId());

        return projections.stream()
                .map(this::convertProjectionToUserBookingDto)
                .collect(Collectors.toList());
    }

    /**
     * Get the last (most recent) booking for the current user (optimized)
     * Returns the booking with the latest createdAt timestamp
     * Returns null if user has no bookings
     */
    public UserBookingDto getLastUserBooking() {
        User currentUser = authUtil.getCurrentUser();
        com.hitendra.turf_booking_backend.repository.projection.UserBookingProjection projection =
                bookingRepository.findLastUserBookingProjected(currentUser.getId());

        if (projection == null) {
            return null;
        }

        return convertProjectionToUserBookingDto(projection);
    }

    private UserBookingDto convertProjectionToUserBookingDto(
            com.hitendra.turf_booking_backend.repository.projection.UserBookingProjection projection) {
        List<UserBookingDto.SlotTimeDto> slotTimes = List.of(UserBookingDto.SlotTimeDto.builder()
                .startTime(projection.getStartTime().toString())
                .endTime(projection.getEndTime().toString())
                .build());

        // Extract paid and due amounts from projection
        Double paidAmount = projection.getOnlineAmountPaid() != null
                ? projection.getOnlineAmountPaid().doubleValue()
                : 0.0;
        Double dueAmount = projection.getVenueAmountDue() != null
                ? projection.getVenueAmountDue().doubleValue()
                : (projection.getAmount() != null ? projection.getAmount() - paidAmount : 0.0);

        return UserBookingDto.builder()
                .id(projection.getId())
                .reference(projection.getReference())
                .serviceId(projection.getServiceId())
                .serviceName(projection.getServiceName())
                .resourceId(projection.getResourceId())
                .resourceName(projection.getResourceName())
                .status(projection.getStatus())
                .date(projection.getBookingDate())
                .slots(slotTimes)
                .totalAmount(projection.getAmount())
                .paidAmount(paidAmount)
                .dueAmount(dueAmount)
                .createdAt(projection.getCreatedAt())
                .build();
    }

    /**
     * Create booking by admin
     */
    public BookingResponseDto createAdminBooking(AdminBookingRequestDTO request) {
        AdminProfile adminProfile = authUtil.getCurrentAdminProfile();

        // Validate time range
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new RuntimeException("Start time must be before end time");
        }

        // Fetch resource
        ServiceResource resource = serviceResourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new RuntimeException("Resource not found"));

        if (!resource.isEnabled()) {
            throw new RuntimeException("Resource is not available for booking");
        }

        Service service = resource.getService();

        if (request.getBookingDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Cannot book slots for past dates");
        }

        // Check for overlapping bookings
        List<Booking> overlappingBookings = bookingRepository.findOverlappingBookings(
                request.getResourceId(),
                request.getBookingDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (!overlappingBookings.isEmpty()) {
            throw new RuntimeException("Selected time range overlaps with an existing booking");
        }

        // Calculate total amount with taxes and fees using PricingService
        PriceBreakdownDto priceBreakdown = pricingService.calculatePriceBreakdownForTimeRange(
                request.getResourceId(),
                request.getStartTime(),
                request.getEndTime(),
                request.getBookingDate()
        );
        double totalAmount = priceBreakdown.getTotalAmount();

        Booking booking = Booking.builder()
                .user(null)
                .service(service)
                .resource(resource)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .bookingDate(request.getBookingDate())
                .amount(totalAmount)
                .reference(generateBookingReference())
                .status(BookingStatus.CONFIRMED)  // Admin bookings are auto-confirmed
                .adminProfile(adminProfile)
                .createdAt(java.time.Instant.now())
                .paymentSource(PaymentSource.BY_ADMIN)
                .build();

        Booking saved = bookingRepository.save(booking);

        log.info("Admin {} created booking {} from {} to {}",
                adminProfile.getId(), saved.getReference(), request.getStartTime(), request.getEndTime());

        return convertToResponseDto(saved);
    }

    /**
     * Manually approve a booking
     */
    public BookingResponseDto approveBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only PENDING bookings can be approved. Current status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking savedBooking = bookingRepository.save(booking);

        // Send notification
        sendBookingNotifications(savedBooking);

        return convertToResponseDto(savedBooking);
    }

    /**
     * Mark a booking as completed (service has been delivered)
     * Only CONFIRMED bookings can be marked as completed
     */
    public BookingResponseDto completeBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Only CONFIRMED bookings can be marked as completed. Current status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.COMPLETED);
        Booking savedBooking = bookingRepository.save(booking);

        log.info("Booking {} marked as completed", bookingId);

        return convertToResponseDto(savedBooking);
    }

    /**
     * Get pending bookings with detailed info
     */
    public PaginatedResponse<PendingBookingDto> getPendingBookingsWithDetails(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Booking> bookingPage = bookingRepository.findByStatus(BookingStatus.PENDING, pageable);

        List<PendingBookingDto> content = bookingPage.getContent().stream()
                .map(this::convertToPendingBookingDto)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(
                content,
                bookingPage.getNumber(),
                bookingPage.getSize(),
                bookingPage.getTotalElements(),
                bookingPage.getTotalPages(),
                bookingPage.isLast()
        );
    }

    // ==================== Helper Methods ====================

    private Booking findBookingByReference(String reference) {
        return bookingRepository.findByReference(reference)
                .orElseThrow(() -> new RuntimeException("Booking not found with reference: " + reference));
    }

    private String generateBookingReference() {
        return "BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void sendBookingNotifications(Booking booking) {
        String slotDetails = booking.getStartTime() + " - " + booking.getEndTime();

        String resourceName = booking.getResource() != null ? booking.getResource().getName() : "";

        if (booking.getUser() != null) {
            // Send SMS notification
            String userMessage = String.format(
                    "Booking confirmed! Service: %s, Resource: %s, Date: %s, Slots: %s, Total: ₹%.2f, Reference: %s",
                    booking.getService().getName(),
                    resourceName,
                    booking.getBookingDate(),
                    slotDetails,
                    booking.getAmount(),
                    booking.getReference()
            );
            smsService.sendBookingConfirmation(booking.getUser().getPhone(), userMessage);

            // Send detailed email notification
            if (booking.getUser().getEmail() != null && !booking.getUser().getEmail().isEmpty()) {
                try {
                    EmailService.BookingDetails bookingDetails = EmailService.BookingDetails.builder()
                            .reference(booking.getReference())
                            .serviceName(booking.getService().getName())
                            .resourceName(resourceName)
                            .bookingDate(booking.getBookingDate().toString())
                            .startTime(booking.getStartTime().toString())
                            .endTime(booking.getEndTime().toString())
                            .totalAmount(booking.getAmount())
                            .onlineAmountPaid(booking.getOnlineAmountPaid() != null ? booking.getOnlineAmountPaid().doubleValue() : booking.getAmount())
                            .venueAmountDue(booking.getVenueAmountDue() != null ? booking.getVenueAmountDue().doubleValue() : 0.0)
                            .location(booking.getService().getLocation())
                            .contactNumber(booking.getService().getContactNumber())
                            .build();

                    emailService.sendBookingConfirmationEmail(
                            booking.getUser().getEmail(),
                            booking.getUser().getName(),
                            bookingDetails
                    );
                } catch (Exception e) {
                    log.error("Failed to send booking confirmation email for booking {}", booking.getReference(), e);
                }
            }
        }

        log.info("Booking notification sent for {}", booking.getReference());
    }

    private UserBookingDto convertToUserBookingDto(Booking booking) {
        List<UserBookingDto.SlotTimeDto> slotTimes = List.of(UserBookingDto.SlotTimeDto.builder()
                .startTime(booking.getStartTime().toString())
                .endTime(booking.getEndTime().toString())
                .build());

        // Extract paid and due amounts from booking
        Double paidAmount = booking.getOnlineAmountPaid() != null
                ? booking.getOnlineAmountPaid().doubleValue()
                : 0.0;
        Double dueAmount = booking.getVenueAmountDue() != null
                ? booking.getVenueAmountDue().doubleValue()
                : (booking.getAmount() != null ? booking.getAmount() - paidAmount : 0.0);

        return UserBookingDto.builder()
                .id(booking.getId())
                .reference(booking.getReference())
                .serviceId(booking.getService().getId())
                .serviceName(booking.getService().getName())
                .resourceId(booking.getResource() != null ? booking.getResource().getId() : null)
                .resourceName(booking.getResource() != null ? booking.getResource().getName() : null)
                .status(booking.getStatus().name())
                .date(booking.getBookingDate())
                .slots(slotTimes)
                .totalAmount(booking.getAmount())
                .paidAmount(paidAmount)
                .dueAmount(dueAmount)
                .createdAt(booking.getCreatedAt())
                .build();
    }

    /**
     * Convert BookingListProjection to BookingResponseDto (optimized - uses projection data)
     */
    private BookingResponseDto convertProjectionToResponseDto(
            com.hitendra.turf_booking_backend.repository.projection.BookingListProjection projection) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setId(projection.getId());
        dto.setReference(projection.getReference());
        dto.setServiceId(projection.getServiceId());
        dto.setServiceName(projection.getServiceName());
        dto.setBookingDate(projection.getBookingDate());
        dto.setCreatedAt(projection.getCreatedAt());
        dto.setStartTime(projection.getStartTime().toString());
        dto.setEndTime(projection.getEndTime().toString());
        dto.setStatus(projection.getStatus());

        if (projection.getResourceId() != null) {
            dto.setResourceId(projection.getResourceId());
            dto.setResourceName(projection.getResourceName());
        }

        // Set user info - if null, it's a walk-in booking (admin-created manual booking)
        if (projection.getUserId() != null) {
            BookingResponseDto.UserInfo userInfo = BookingResponseDto.UserInfo.builder()
                    .id(projection.getUserId())
                    .name(projection.getUserName())
                    .email(projection.getUserEmail())
                    .phone(projection.getUserPhone())
                    .build();
            dto.setUser(userInfo);
        } else {
            // Admin-created manual booking (no user) - show as WALK-IN BOOKING
            BookingResponseDto.UserInfo walkinUser = BookingResponseDto.UserInfo.builder()
                    .id(null)
                    .name("WALK-IN BOOKING")
                    .email(null)
                    .phone(null)
                    .build();
            dto.setUser(walkinUser);
        }

        // Use stored amounts from projection (already calculated during booking creation)
        Double totalAmount = projection.getAmount();
        if (totalAmount != null) {
            double onlineAmount = projection.getOnlineAmountPaid() != null
                    ? projection.getOnlineAmountPaid().doubleValue()
                    : Math.round(totalAmount * onlinePaymentPercent) / 100.0;
            double venueAmount = projection.getVenueAmountDue() != null
                    ? projection.getVenueAmountDue().doubleValue()
                    : Math.round((totalAmount - onlineAmount) * 100.0) / 100.0;
            Boolean venueAmountCollected = projection.getVenueAmountCollected() != null
                    ? projection.getVenueAmountCollected()
                    : false;

            // Calculate breakdown from total (assume 2% platform fee)
            double platformFeePercent = 2.0;
            double slotSubtotal = totalAmount / (1 + platformFeePercent / 100.0);
            double platformFee = totalAmount - slotSubtotal;

            // Round to 2 decimal places
            slotSubtotal = Math.round(slotSubtotal * 100.0) / 100.0;
            platformFee = Math.round(platformFee * 100.0) / 100.0;
            totalAmount = Math.round(totalAmount * 100.0) / 100.0;

            BookingResponseDto.AmountBreakdown amountBreakdown = BookingResponseDto.AmountBreakdown.builder()
                    .slotSubtotal(slotSubtotal)
                    .platformFeePercent(platformFeePercent)
                    .platformFee(platformFee)
                    .totalAmount(totalAmount)
                    .onlinePaymentPercent(onlinePaymentPercent)
                    .onlineAmount(onlineAmount)
                    .venueAmount(venueAmount)
                    .venueAmountCollected(venueAmountCollected)
                    .currency("INR")
                    .build();

            dto.setAmountBreakdown(amountBreakdown);
        }

        return dto;
    }

    private BookingResponseDto convertToResponseDto(Booking booking) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setId(booking.getId());
        dto.setReference(booking.getReference());
        dto.setServiceId(booking.getService().getId());
        dto.setServiceName(booking.getService().getName());
        dto.setBookingDate(booking.getBookingDate());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setStartTime(booking.getStartTime().toString());
        dto.setEndTime(booking.getEndTime().toString());
        dto.setStatus(booking.getStatus().name());

        if (booking.getResource() != null) {
            dto.setResourceId(booking.getResource().getId());
            dto.setResourceName(booking.getResource().getName());
        }

        // Set user info - if null, it's a walk-in booking (admin-created manual booking)
        if (booking.getUser() != null) {
            BookingResponseDto.UserInfo userInfo = BookingResponseDto.UserInfo.builder()
                    .id(booking.getUser().getId())
                    .name(booking.getUser().getName())
                    .email(booking.getUser().getEmail())
                    .phone(booking.getUser().getPhone())
                    .build();
            dto.setUser(userInfo);
        } else {
            // Admin-created manual booking (no user) - show as WALK-IN BOOKING
            BookingResponseDto.UserInfo walkinUser = BookingResponseDto.UserInfo.builder()
                    .id(null)
                    .name("WALK-IN BOOKING")
                    .email(null)
                    .phone(null)
                    .build();
            dto.setUser(walkinUser);
        }

        // Calculate accurate price breakdown using PricingService
        if (booking.getResource() != null && booking.getAmount() != null) {
            try {
                // Use PricingService for accurate calculation
                PriceBreakdownDto priceBreakdown = pricingService.calculatePriceBreakdownForTimeRange(
                        booking.getResource().getId(),
                        booking.getStartTime(),
                        booking.getEndTime(),
                        booking.getBookingDate()
                );

                // Use calculated values from PricingService
                double slotSubtotal = priceBreakdown.getSubtotal() != null ? priceBreakdown.getSubtotal() : 0.0;
                double platformFeePercent = priceBreakdown.getConvenienceFeeRate() != null ? priceBreakdown.getConvenienceFeeRate() : 2.0;
                double platformFee = priceBreakdown.getConvenienceFee() != null ? priceBreakdown.getConvenienceFee() : 0.0;

                // Use the stored amount as the authoritative total (in case pricing rules changed)
                double storedTotal = booking.getAmount();

                // If calculated total differs significantly from stored total,
                // use stored total and recalculate breakdown proportionally
                double calculatedTotal = priceBreakdown.getTotalAmount() != null ? priceBreakdown.getTotalAmount() : 0.0;

                if (Math.abs(calculatedTotal - storedTotal) > 0.01) {
                    // Pricing rules may have changed, so derive breakdown from stored amount
                    // Using the formula: storedTotal = subtotal * (1 + feeRate/100)
                    // Therefore: subtotal = storedTotal / (1 + feeRate/100)
                    slotSubtotal = storedTotal / (1 + platformFeePercent / 100.0);
                    platformFee = storedTotal - slotSubtotal;
                    log.debug("Price recalculated for booking {}: stored={}, calculated={}",
                            booking.getId(), storedTotal, calculatedTotal);
                }

                // Round to 2 decimal places for monetary precision
                slotSubtotal = Math.round(slotSubtotal * 100.0) / 100.0;
                platformFee = Math.round(platformFee * 100.0) / 100.0;
                storedTotal = Math.round(storedTotal * 100.0) / 100.0;

                // Calculate online and venue amounts
                double onlineAmount;
                double venueAmount;

                if (booking.getOnlineAmountPaid() != null) {
                    // Use stored value if available
                    onlineAmount = booking.getOnlineAmountPaid().doubleValue();
                    venueAmount = Math.round((storedTotal - onlineAmount) * 100.0) / 100.0;
                } else {
                    // Calculate from percentage (for backward compatibility)
                    onlineAmount = Math.round(storedTotal * onlinePaymentPercent) / 100.0;
                    venueAmount = Math.round((storedTotal - onlineAmount) * 100.0) / 100.0;
                }

                Boolean venueAmountCollected = booking.getVenueAmountCollected() != null ? booking.getVenueAmountCollected() : false;

                BookingResponseDto.AmountBreakdown amountBreakdown = BookingResponseDto.AmountBreakdown.builder()
                        .slotSubtotal(slotSubtotal)
                        .platformFeePercent(platformFeePercent)
                        .platformFee(platformFee)
                        .totalAmount(storedTotal)
                        .onlinePaymentPercent(onlinePaymentPercent)
                        .onlineAmount(onlineAmount)
                        .venueAmount(venueAmount)
                        .venueAmountCollected(venueAmountCollected)
                        .currency("INR")
                        .build();

                dto.setAmountBreakdown(amountBreakdown);
            } catch (Exception e) {
                log.warn("Failed to calculate price breakdown for booking {}: {}", booking.getId(), e.getMessage());
                // Fallback: derive from stored amount with default 2% fee
                double storedTotal = booking.getAmount();
                double platformFeePercent = 2.0;
                double slotSubtotal = storedTotal / (1 + platformFeePercent / 100.0);
                double platformFee = storedTotal - slotSubtotal;

                // Round to 2 decimal places
                slotSubtotal = Math.round(slotSubtotal * 100.0) / 100.0;
                platformFee = Math.round(platformFee * 100.0) / 100.0;
                storedTotal = Math.round(storedTotal * 100.0) / 100.0;

                // Calculate online and venue amounts for fallback
                double onlineAmount;
                double venueAmount;

                if (booking.getOnlineAmountPaid() != null) {
                    onlineAmount = booking.getOnlineAmountPaid().doubleValue();
                    venueAmount = Math.round((storedTotal - onlineAmount) * 100.0) / 100.0;
                } else {
                    onlineAmount = Math.round(storedTotal * onlinePaymentPercent) / 100.0;
                    venueAmount = Math.round((storedTotal - onlineAmount) * 100.0) / 100.0;
                }

                Boolean venueAmountCollected = booking.getVenueAmountCollected() != null ? booking.getVenueAmountCollected() : false;

                BookingResponseDto.AmountBreakdown fallbackBreakdown = BookingResponseDto.AmountBreakdown.builder()
                        .slotSubtotal(slotSubtotal)
                        .platformFeePercent(platformFeePercent)
                        .platformFee(platformFee)
                        .totalAmount(storedTotal)
                        .onlinePaymentPercent(onlinePaymentPercent)
                        .onlineAmount(onlineAmount)
                        .venueAmount(venueAmount)
                        .venueAmountCollected(venueAmountCollected)
                        .currency("INR")
                        .build();
                dto.setAmountBreakdown(fallbackBreakdown);
            }
        }

        return dto;
    }

    private PendingBookingDto convertToPendingBookingDto(Booking booking) {
        PendingBookingDto.UserInfo userInfo;
        if (booking.getUser() != null) {
            userInfo = PendingBookingDto.UserInfo.builder()
                    .id(booking.getUser().getId())
                    .name(booking.getUser().getName())
                    .email(booking.getUser().getEmail())
                    .phone(booking.getUser().getPhone())
                    .build();
        } else {
            // Admin-created manual booking (no user) - show as WALK-IN BOOKING
            userInfo = PendingBookingDto.UserInfo.builder()
                    .id(null)
                    .name("WALK-IN BOOKING")
                    .email(null)
                    .phone(null)
                    .build();
        }

        return PendingBookingDto.builder()
                .id(booking.getId())
                .reference(booking.getReference())
                .serviceId(booking.getService().getId())
                .serviceName(booking.getService().getName())
                .resourceId(booking.getResource() != null ? booking.getResource().getId() : null)
                .resourceName(booking.getResource() != null ? booking.getResource().getName() : null)
                .startTime(booking.getStartTime().toString())
                .endTime(booking.getEndTime().toString())
                .bookingDate(booking.getBookingDate())
                .amount(booking.getAmount())
                .status(booking.getStatus().name())
                .createdAt(booking.getCreatedAt())
                .user(userInfo)
                .build();
    }
}
