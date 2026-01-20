package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.entity.Booking;

public interface PaymentService {
    boolean initiatePayment(Booking booking, Object paymentDetails);
    void handlePaymentSuccess(String reference);
    void handlePaymentFailure(String reference);
}
