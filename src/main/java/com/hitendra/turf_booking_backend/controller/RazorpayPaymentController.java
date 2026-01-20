package com.hitendra.turf_booking_backend.controller;

import com.hitendra.turf_booking_backend.dto.booking.BookingStatusResponse;
import com.hitendra.turf_booking_backend.dto.payment.RazorpayOrderResponse;
import com.hitendra.turf_booking_backend.dto.payment.RazorpayPaymentVerificationRequest;
import com.hitendra.turf_booking_backend.service.RazorpayPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/razorpay")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Razorpay Payment", description = "Razorpay payment gateway integration APIs")
public class RazorpayPaymentController {

    private final RazorpayPaymentService razorpayPaymentService;

    @PostMapping("/create-order/{bookingId}")
    @Operation(summary = "Create Razorpay Order", description = "Create a Razorpay order for a booking")
    public ResponseEntity<RazorpayOrderResponse> createOrder(@PathVariable Long bookingId) {
        log.info("Received request to create Razorpay order for booking ID: {}", bookingId);
        RazorpayOrderResponse response = razorpayPaymentService.createOrder(bookingId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/booking-status/{bookingId}")
    @Operation(
        summary = "Get Booking Status",
        description = "Poll booking and payment status. Used by frontend loading screens during payment."
    )
    public ResponseEntity<BookingStatusResponse> getBookingStatus(@PathVariable Long bookingId) {
        log.info("Received request to fetch booking status for booking ID: {}", bookingId);
        BookingStatusResponse response = razorpayPaymentService.getBookingStatus(bookingId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-payment")
    @Operation(
        summary = "Verify Payment Signature (Informational Only)",
        description = "Verifies Razorpay payment signature. NOTE: Booking confirmation happens ONLY via webhook. " +
                     "This endpoint is for frontend validation only."
    )
    public ResponseEntity<Map<String, Object>> verifyPayment(
            @Valid @RequestBody RazorpayPaymentVerificationRequest request) {
        log.info("Received request to verify payment for order ID: {}", request.getRazorpayOrderId());

        boolean isValid = razorpayPaymentService.verifyPaymentSignature(request);

        if (isValid) {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Payment signature verified. Awaiting webhook confirmation for booking."
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Payment verification failed"
            ));
        }
    }

    @PostMapping("/webhook")
    @Operation(summary = "Razorpay Webhook", description = "Handle Razorpay webhook events")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        log.info("Received Razorpay webhook");

        try {
            razorpayPaymentService.handleWebhook(payload, signature);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            log.error("Failed to process webhook", e);
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "error", e.getMessage()));
        }
    }

    @GetMapping("/payment/{paymentId}")
    @Operation(summary = "Get Payment Details", description = "Fetch payment details from Razorpay")
    public ResponseEntity<?> getPaymentDetails(@PathVariable String paymentId) {
        log.info("Received request to fetch payment details for payment ID: {}", paymentId);
        try {
            var payment = razorpayPaymentService.getPaymentDetails(paymentId);
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get Order Details", description = "Fetch order details from Razorpay")
    public ResponseEntity<?> getOrderDetails(@PathVariable String orderId) {
        log.info("Received request to fetch order details for order ID: {}", orderId);
        try {
            var order = razorpayPaymentService.getOrderDetails(orderId);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

