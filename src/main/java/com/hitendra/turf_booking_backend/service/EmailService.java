package com.hitendra.turf_booking_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final GmailService gmailService;

    @Value("${email.from-email}")
    private String fromEmail;

    @Value("${email.from-name}")
    private String fromName;

    /**
     * Send OTP email to user using Gmail API
     */
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            log.info("Sending OTP email to: {}", toEmail);

            String subject = "Your Hyper OTP Code";
            String htmlBody = buildOtpEmailBody(otpCode);

            gmailService.sendEmail(toEmail, subject, htmlBody);

            log.info("OTP email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send OTP email: " + e.getMessage(), e);
        }
    }

    /**
     * Send booking confirmation email with detailed information
     */
    public void sendBookingConfirmationEmail(String toEmail, String userName, BookingDetails bookingDetails) {
        try {
            log.info("Sending booking confirmation email to: {}", toEmail);

            String subject = "Booking Confirmed - " + bookingDetails.getServiceName();
            String htmlBody = buildBookingConfirmationEmailBody(userName, bookingDetails);

            gmailService.sendEmail(toEmail, subject, htmlBody);

            log.info("Booking confirmation email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send booking confirmation email to: {}", toEmail, e);
            // Don't throw exception for email failure
        }
    }

    /**
     * Build OTP email body with branding
     */
    private String buildOtpEmailBody(String otpCode) {
        return """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <title>Your Hyper verification code</title>
    </head>
    <body style="margin:0; padding:0; background-color:#ffffff; font-family:Arial, Helvetica, sans-serif; color:#000000;">
        <table width="100%%" cellpadding="0" cellspacing="0">
            <tr>
                <td align="center" style="padding:24px;">
                    <table width="480" cellpadding="0" cellspacing="0" style="border:1px solid #e5e7eb;">
                        
                        <!-- Header -->
                        <tr>
                            <td style="padding:16px; font-size:18px; font-weight:bold;">
                                Hyper verification code
                            </td>
                        </tr>

                        <!-- Body -->
                        <tr>
                            <td style="padding:16px; font-size:14px; line-height:1.6;">
                                <p>Hello,</p>

                                <p>
                                    Your one-time verification code for Hyper is:
                                </p>

                                <p style="font-size:22px; font-weight:bold; letter-spacing:3px; margin:16px 0;">
                                    %s
                                </p>

                                <p>
                                    This code will expire in 5 minutes.
                                </p>

                                <p>
                                    If you did not request this code, you can safely ignore this email.
                                </p>

                                <p style="margin-top:24px;">
                                    Regards,<br>
                                    Hyper Team
                                </p>
                            </td>
                        </tr>

                        <!-- Footer -->
                        <tr>
                            <td style="padding:12px; font-size:12px; color:#555555; border-top:1px solid #e5e7eb;">
                                This is an automated message. Replies are not monitored.
                            </td>
                        </tr>

                    </table>
                </td>
            </tr>
        </table>
    </body>
    </html>
    """.formatted(otpCode);
    }


    /**
     * Build booking confirmation email body with card UI
     */
    private String buildBookingConfirmationEmailBody(String userName, BookingDetails booking) {
        String displayName = userName != null && !userName.isEmpty() ? userName : "there";

        return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <title>Booking Confirmation</title>
    </head>
    <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">
        <table width="100%%" cellpadding="0" cellspacing="0">
            <tr>
                <td align="center" style="padding: 30px 0;">
                    <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff; border-radius:12px; box-shadow:0 4px 15px rgba(0,0,0,0.08);">
                        
                        <!-- Success Header -->
                        <tr>
                            <td style="background:linear-gradient(135deg, #10b981 0%%, #059669 100%%); padding:32px; text-align:center; border-radius:12px 12px 0 0;">
                                <div style="font-size:48px; margin-bottom:12px;">✓</div>
                                <h1 style="color:#ffffff; margin:0 0 8px 0; font-size:26px;">Booking Confirmed!</h1>
                                <p style="color:#d1fae5; margin:0; font-size:15px;">Your booking has been successfully confirmed</p>
                            </td>
                        </tr>

                        <!-- Greeting -->
                        <tr>
                            <td style="padding:24px 32px 16px 32px;">
                                <p style="font-size:16px; margin:0; color:#374151;">Hello %s,</p>
                            </td>
                        </tr>

                        <!-- Booking Reference Card -->
                        <tr>
                            <td style="padding:0 32px 24px 32px;">
                                <div style="background:#f0fdf4; border-left:4px solid #10b981; padding:16px 20px; border-radius:6px;">
                                    <p style="margin:0 0 6px 0; font-size:13px; color:#6b7280; font-weight:600;">BOOKING REFERENCE</p>
                                    <p style="margin:0; font-size:20px; color:#065f46; font-weight:bold; letter-spacing:1px;">%s</p>
                                </div>
                            </td>
                        </tr>

                        <!-- Booking Details Card -->
                        <tr>
                            <td style="padding:0 32px 24px 32px;">
                                <div style="border:1px solid #e5e7eb; border-radius:8px; overflow:hidden;">
                                    
                                    <!-- Service Name Header -->
                                    <div style="background:#f9fafb; padding:16px 20px; border-bottom:1px solid #e5e7eb;">
                                        <p style="margin:0; font-size:18px; font-weight:600; color:#111827;">%s</p>
                                        %s
                                    </div>
                                    
                                    <!-- Details Grid -->
                                    <div style="padding:20px;">
                                        <table width="100%%" cellpadding="0" cellspacing="0">
                                            <tr>
                                                <td style="padding:12px 0; border-bottom:1px solid #f3f4f6;">
                                                    <p style="margin:0; font-size:13px; color:#6b7280;">📅 Date</p>
                                                    <p style="margin:4px 0 0 0; font-size:15px; color:#111827; font-weight:600;">%s</p>
                                                </td>
                                                <td style="padding:12px 0; border-bottom:1px solid #f3f4f6; text-align:right;">
                                                    <p style="margin:0; font-size:13px; color:#6b7280;">⏰ Time</p>
                                                    <p style="margin:4px 0 0 0; font-size:15px; color:#111827; font-weight:600;">%s - %s</p>
                                                </td>
                                            </tr>
                                            %s
                                            <tr>
                                                <td colspan="2" style="padding:16px 0 8px 0;">
                                                    <p style="margin:0; font-size:13px; color:#6b7280;">💰 Amount Paid</p>
                                                    <p style="margin:4px 0 0 0; font-size:22px; color:#059669; font-weight:bold;">₹%s</p>
                                                </td>
                                            </tr>
                                        </table>
                                    </div>
                                </div>
                            </td>
                        </tr>

                        %s

                        <!-- Important Info -->
                        <tr>
                            <td style="padding:0 32px 24px 32px;">
                                <div style="background:#fef3c7; border-left:4px solid #f59e0b; padding:16px 20px; border-radius:6px;">
                                    <p style="margin:0 0 8px 0; font-size:14px; color:#92400e; font-weight:600;">📌 Important Information</p>
                                    <ul style="margin:0; padding-left:20px; color:#78350f; font-size:14px; line-height:1.6;">
                                        <li style="margin-bottom:6px;">Please arrive 5-10 minutes before your slot time</li>
                                        <li style="margin-bottom:6px;">Keep your booking reference handy</li>
                                        <li>For any queries, contact the venue directly</li>
                                    </ul>
                                </div>
                            </td>
                        </tr>

                        <!-- Footer Message -->
                        <tr>
                            <td style="padding:0 32px 32px 32px;">
                                <p style="font-size:15px; color:#374151; margin:0 0 24px 0; line-height:1.6;">
                                    Thank you for booking with Hyper! We hope you have a great experience.
                                </p>
                                <p style="margin:0; font-size:14px; color:#6b7280;">
                                    Best regards,<br/>
                                    <strong style="color:#111827;">Hyper Team</strong>
                                </p>
                            </td>
                        </tr>

                        <!-- Footer -->
                        <tr>
                            <td style="padding:24px 32px; background:#f9fafb; font-size:12px; color:#9ca3af; text-align:center; border-radius:0 0 12px 12px; border-top:1px solid #e5e7eb;">
                                <p style="margin:0 0 8px 0;">Need help? Contact us at <a href="mailto:support@hyper.com" style="color:#10b981; text-decoration:none;">support@hyper.com</a></p>
                                <p style="margin:0;">This is an automated message. Please do not reply to this email.</p>
                            </td>
                        </tr>

                    </table>
                </td>
            </tr>
        </table>
    </body>
    </html>
    """.formatted(
            displayName,
            booking.getReference(),
            booking.getServiceName(),
            booking.getResourceName() != null ?
                "<p style=\"margin:4px 0 0 0; font-size:14px; color:#6b7280;\">Resource: " + booking.getResourceName() + "</p>" : "",
            booking.getBookingDate(),
            booking.getStartTime(),
            booking.getEndTime(),
            booking.getLocation() != null ?
                "<tr><td colspan=\"2\" style=\"padding:12px 0; border-bottom:1px solid #f3f4f6;\"><p style=\"margin:0; font-size:13px; color:#6b7280;\">📍 Location</p><p style=\"margin:4px 0 0 0; font-size:15px; color:#111827; font-weight:500;\">" + booking.getLocation() + "</p></td></tr>" : "",
            String.format("%.2f", booking.getTotalAmount()),
            booking.getContactNumber() != null ?
                "<tr><td style=\"padding:0 32px 24px 32px;\"><div style=\"background:#eff6ff; border-left:4px solid #3b82f6; padding:16px 20px; border-radius:6px;\"><p style=\"margin:0 0 6px 0; font-size:13px; color:#1e40af; font-weight:600;\">📞 Venue Contact</p><p style=\"margin:0; font-size:16px; color:#1e3a8a; font-weight:600;\">" + booking.getContactNumber() + "</p></div></td></tr>" : ""
        );
    }

    /**
     * DTO class for booking details
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BookingDetails {
        private String reference;
        private String serviceName;
        private String resourceName;
        private String bookingDate;
        private String startTime;
        private String endTime;
        private Double totalAmount;
        private String location;
        private String contactNumber;
    }
}

