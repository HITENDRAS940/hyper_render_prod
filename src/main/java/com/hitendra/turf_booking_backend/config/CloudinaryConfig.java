package com.hitendra.turf_booking_backend.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {

        log.info("Initializing Cloudinary with cloud name: {}", cloudName);

        if (cloudName == null || cloudName.isEmpty() || cloudName.equals("your_cloud_name")) {
            log.error("Cloudinary cloud name is not configured properly!");
            throw new IllegalStateException("Cloudinary cloud name must be configured in application.properties");
        }

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your_api_key")) {
            log.error("Cloudinary API key is not configured properly!");
            throw new IllegalStateException("Cloudinary API key must be configured in application.properties");
        }

        if (apiSecret == null || apiSecret.isEmpty() || apiSecret.equals("your_api_secret")) {
            log.error("Cloudinary API secret is not configured properly!");
            throw new IllegalStateException("Cloudinary API secret must be configured in application.properties");
        }

        Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));

        log.info("Cloudinary initialized successfully");
        return cloudinary;
    }
}
