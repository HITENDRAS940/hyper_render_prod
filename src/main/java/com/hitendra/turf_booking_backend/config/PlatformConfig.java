package com.hitendra.turf_booking_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "platform")
public class PlatformConfig {

    private BigDecimal advancePercentage;

    public BigDecimal getAdvancePercentage() {
        return advancePercentage;
    }

    public void setAdvancePercentage(BigDecimal advancePercentage) {
        this.advancePercentage = advancePercentage;
    }
}

