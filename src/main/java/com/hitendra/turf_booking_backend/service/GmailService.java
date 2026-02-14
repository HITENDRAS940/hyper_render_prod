package com.hitendra.turf_booking_backend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Properties;

/**
 * Gmail API Service for sending emails.
 *
 * Uses environment variables with stored refresh token:
 * - GMAIL_CLIENT_ID
 * - GMAIL_CLIENT_SECRET
 * - GMAIL_REFRESH_TOKEN (obtained from local OAuth flow)
 */
@Service
@Slf4j
public class GmailService {

    private static final String APPLICATION_NAME = "Hyper Backend";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    // Environment variables for production
    @Value("${gmail.client-id:}")
    private String clientId;

    @Value("${gmail.client-secret:}")
    private String clientSecret;

    @Value("${gmail.refresh-token:}")
    private String refreshToken;

    @Value("${gmail.from-email:gethyperindia@gmail.com}")
    private String fromEmail;

    private Gmail gmail;

    @PostConstruct
    public void init() throws Exception {
        log.info("Initializing Gmail Service...");

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        if (!hasEnvCredentials()) {
            throw new RuntimeException(
                "Gmail API not configured. Set GMAIL_CLIENT_ID, GMAIL_CLIENT_SECRET, " +
                "and GMAIL_REFRESH_TOKEN environment variables."
            );
        }

        log.info("Using environment variables for Gmail API");
        initWithEnvCredentials(httpTransport);

        log.info("Gmail Service initialized successfully");
    }

    /**
     * Check if environment credentials are available
     */
    private boolean hasEnvCredentials() {
        return clientId != null && !clientId.isEmpty()
                && clientSecret != null && !clientSecret.isEmpty()
                && refreshToken != null && !refreshToken.isEmpty();
    }

    /**
     * Initialize Gmail API using environment variables
     */
    @SuppressWarnings("deprecation")
    private void initWithEnvCredentials(HttpTransport httpTransport) throws Exception {
        log.info("Building Gmail client with refresh token...");

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(refreshToken);

        if (credential.refreshToken()) {
            log.info("Successfully refreshed access token");
        } else {
            log.warn("Could not refresh token - will attempt on first API call");
        }

        gmail = new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public void sendEmail(String to, String subject, String body) throws Exception {
        log.info("Sending email to: {} with subject: {}", to, subject);

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(fromEmail, "Hyper India"));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setContent(body, "text/html; charset=utf-8");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);

        Message message = new Message();
        message.setRaw(
                Base64.getUrlEncoder().encodeToString(buffer.toByteArray())
        );

        gmail.users().messages().send("me", message).execute();

        log.info("Email sent successfully to: {}", to);
    }
}
