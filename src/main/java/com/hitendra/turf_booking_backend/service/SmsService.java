package com.hitendra.turf_booking_backend.service;

public interface SmsService {
    void sendOtp(String phone, String otp);
    void sendBookingConfirmation(String phone, String message);
}
