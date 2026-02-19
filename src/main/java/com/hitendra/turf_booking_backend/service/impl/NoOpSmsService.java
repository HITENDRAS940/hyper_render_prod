package com.hitendra.turf_booking_backend.service.impl;

import com.hitendra.turf_booking_backend.service.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * No-op SMS service implementation.
 * SMS functionality is disabled - all methods do nothing.
 *
 * To enable SMS in the future:
 * 1. Implement a proper SMS service (MSG91, Twilio, AWS SNS, etc.)
 * 2. Replace this implementation or make it conditional
 */
@Service
@Slf4j
public class NoOpSmsService implements SmsService {

    public NoOpSmsService() {
        log.info("SMS Service is disabled. No SMS notifications will be sent.");
    }

    @Override
    public void sendOtp(String phone, String otp) {
        // Do nothing - SMS disabled
    }

    @Override
    public void sendBookingConfirmation(String phone, String message) {
        // Do nothing - SMS disabled
    }
}


