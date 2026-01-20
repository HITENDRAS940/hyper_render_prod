package com.hitendra.turf_booking_backend.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

/**
 * Gmail API Service for sending emails.
 *
 * Supports two modes:
 * 1. LOCAL DEVELOPMENT: Uses credentials.json file and interactive OAuth flow
 * 2. PRODUCTION (Render): Uses environment variables with stored refresh token
 *
 * For production, set these environment variables:
 * - GMAIL_CLIENT_ID
 * - GMAIL_CLIENT_SECRET
 * - GMAIL_REFRESH_TOKEN (obtained from local OAuth flow)
 */
@Service
@Slf4j
public class GmailService {

    private static final String APPLICATION_NAME = "Hyper Backend";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES =
            List.of("https://www.googleapis.com/auth/gmail.send");

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

        // Check if we have environment variables (production mode)
        if (hasEnvCredentials()) {
            log.info("Using environment variables for Gmail API (production mode)");
            initWithEnvCredentials(httpTransport);
        } else {
            log.info("Using credentials.json file for Gmail API (development mode)");
            initWithCredentialsFile(httpTransport);
        }

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
     * Initialize Gmail API using environment variables (for production)
     */
    @SuppressWarnings("deprecation")
    private void initWithEnvCredentials(HttpTransport httpTransport) throws Exception {
        log.info("Building Gmail client with refresh token...");

        // Use GoogleCredential with refresh token
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret)
                .build()
                .setRefreshToken(refreshToken);

        // Force token refresh to validate credentials
        if (credential.refreshToken()) {
            log.info("Successfully refreshed access token");
        } else {
            log.warn("Could not refresh token - will attempt on first API call");
        }

        gmail = new Gmail.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Initialize Gmail API using credentials.json file (for local development)
     */
    private void initWithCredentialsFile(HttpTransport httpTransport) throws Exception {
        ClassPathResource resource = new ClassPathResource("credentials.json");

        if (!resource.exists()) {
            log.error("credentials.json not found and no environment variables set");
            throw new RuntimeException(
                "Gmail API not configured. Either:\n" +
                "1. Add credentials.json to src/main/resources/ (for local dev)\n" +
                "2. Set GMAIL_CLIENT_ID, GMAIL_CLIENT_SECRET, GMAIL_REFRESH_TOKEN env vars (for production)"
            );
        }

        InputStream in = resource.getInputStream();
        log.info("credentials.json loaded successfully");

        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        httpTransport,
                        JSON_FACTORY,
                        clientSecrets,
                        SCOPES)
                        .setDataStoreFactory(
                                new FileDataStoreFactory(new File(".tokens")))
                        .setAccessType("offline")
                        .build();

        // Use port 8080 for OAuth callback
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setHost("localhost")
                .setPort(8080)
                .build();

        Credential credential = new AuthorizationCodeInstalledApp(
                flow,
                receiver
        ).authorize("user");

        // Log the refresh token for production setup
        if (credential.getRefreshToken() != null) {
            log.info("=======================================================");
            log.info("IMPORTANT: Save this refresh token for production use!");
            log.info("GMAIL_REFRESH_TOKEN={}", credential.getRefreshToken());
            log.info("=======================================================");
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
