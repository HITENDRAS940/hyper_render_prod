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
     * Send welcome email to new users using Gmail API
     */
    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            log.info("Sending welcome email to: {}", toEmail);

            String subject = "Welcome to Hyper!";
            String htmlBody = buildWelcomeEmailBody(userName);

            gmailService.sendEmail(toEmail, subject, htmlBody);

            log.info("Welcome email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", toEmail, e);
            // Don't throw exception for welcome email failure
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
     * Build welcome email body
     */
    private String buildWelcomeEmailBody(String userName) {
        String displayName = userName != null ? userName : "there";
        return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <title>Welcome to Hyper</title>
    </head>
    <body style="margin:0; padding:0; background-color:#f4f6f8; font-family:Arial, Helvetica, sans-serif;">
        <table width="100%%" cellpadding="0" cellspacing="0">
            <tr>
                <td align="center" style="padding: 30px 0;">
                    <table width="480" cellpadding="0" cellspacing="0" style="background:#ffffff; border-radius:8px; box-shadow:0 4px 10px rgba(0,0,0,0.05);">
                        
                        <!-- Header -->
                        <tr>
                            <td style="background:linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding:24px; text-align:center; border-radius:8px 8px 0 0;">
                                <h1 style="color:#ffffff; margin:0;">Welcome to Hyper! 🎉</h1>
                            </td>
                        </tr>

                        <!-- Body -->
                        <tr>
                            <td style="padding:32px; color:#333333;">
                                <p style="font-size:16px; margin-bottom:8px;">Hello %s,</p>

                                <p style="font-size:15px; line-height:1.6; margin-bottom:24px;">
                                    We're excited to have you on board! Your account has been successfully created.
                                </p>

                                <div style="background:#f0f5ff; padding:20px; border-radius:6px; margin-bottom:24px;">
                                    <p style="margin:0 0 12px 0; font-size:15px; font-weight:600; color:#2563eb;">
                                        Here's what you can do now:
                                    </p>
                                    <ul style="margin:0; padding-left:20px; color:#555555;">
                                        <li style="margin-bottom:8px;">Browse and book sports courts</li>
                                        <li style="margin-bottom:8px;">Manage your bookings</li>
                                        <li>View booking history</li>
                                    </ul>
                                </div>

                                <p style="font-size:15px; margin-bottom:24px;">
                                    Start booking your favorite courts today!
                                </p>

                                <p style="margin-top:32px; font-size:14px;">
                                    Best regards,<br/>
                                    <strong>Hyper Team</strong>
                                </p>
                            </td>
                        </tr>

                        <!-- Footer -->
                        <tr>
                            <td style="padding:20px; background:#f9fafb; font-size:12px; color:#999999; text-align:center; border-radius:0 0 8px 8px;">
                                Need help? Contact us at <a href="mailto:support@hyper.com" style="color:#2563eb; text-decoration:none;">support@hyper.com</a>
                            </td>
                        </tr>

                    </table>
                </td>
            </tr>
        </table>
    </body>
    </html>
    """.formatted(displayName);
    }
}

