package com.hitendra.turf_booking_backend.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name:#{null}}")
    private String cloudName;

    @Value("${cloudinary.api-key:#{null}}")
    private String apiKey;

    @Value("${cloudinary.api-secret:#{null}}")
    private String apiSecret;

    @Bean
    @ConditionalOnProperty(prefix = "cloudinary", name = "cloud-name")
    public Cloudinary cloudinary() {

        log.info("Initializing Cloudinary with cloud name: {}", cloudName);

        if (cloudName == null || cloudName.isEmpty() || cloudName.equals("your_cloud_name")) {
            log.warn("⚠️  Cloudinary cloud name is not configured. Image upload features will be disabled.");
            return null;
        }

        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your_api_key")) {
            log.warn("⚠️  Cloudinary API key is not configured. Image upload features will be disabled.");
            return null;
        }

        if (apiSecret == null || apiSecret.isEmpty() || apiSecret.equals("your_api_secret")) {
            log.warn("⚠️  Cloudinary API secret is not configured. Image upload features will be disabled.");
            return null;
        }

        try {
            Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true
            ));

            log.info("✅ Cloudinary initialized successfully");
            return cloudinary;
        } catch (Exception e) {
            log.error("❌ Failed to initialize Cloudinary: {}. Image upload features will be disabled.", e.getMessage());
            return null;
        }
    }
}
