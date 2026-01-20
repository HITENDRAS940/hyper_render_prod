package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.service.CityResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Calculate distance between two points using Haversine formula
     * Returns distance in kilometers
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in kilometers
    }

    /**
     * Get city name from latitude and longitude using Nominatim (OpenStreetMap) API
     * This is a free reverse geocoding service
     */
    public CityResponse getCityFromCoordinates(double latitude, double longitude) {
        try {
            String url = String.format(
                "https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s&zoom=10&addressdetails=1",
                latitude, longitude
            );

            // Set User-Agent header as required by Nominatim
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "TurfBookingApp/1.0");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            org.springframework.http.ResponseEntity<Map> response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                entity,
                Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
            if (responseBody != null && responseBody.containsKey("address")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> address = (Map<String, Object>) responseBody.get("address");

                String city = extractCity(address);
                String state = (String) address.get("state");
                String country = (String) address.get("country");

                return new CityResponse(city, state, country);
            }

            log.warn("Unable to determine city from coordinates: {}, {}", latitude, longitude);
            return new CityResponse("Unknown", "Unknown", "Unknown");

        } catch (Exception e) {
            log.error("Error getting city from coordinates: {}, {}", latitude, longitude, e);
            return new CityResponse("Unknown", "Unknown", "Unknown");
        }
    }

    /**
     * Extract city name from address components
     * Try multiple fields in order of preference
     */
    private String extractCity(Map<String, Object> address) {
        // Try different possible fields for city name
        if (address.containsKey("city")) {
            return (String) address.get("city");
        } else if (address.containsKey("town")) {
            return (String) address.get("town");
        } else if (address.containsKey("village")) {
            return (String) address.get("village");
        } else if (address.containsKey("municipality")) {
            return (String) address.get("municipality");
        } else if (address.containsKey("county")) {
            return (String) address.get("county");
        }
        return "Unknown";
    }

    /**
     * Extract latitude and longitude from a Google Maps URL
     * Supports various Google Maps URL formats:
     * - https://www.google.com/maps?q=lat,lng
     * - https://www.google.com/maps/@lat,lng,zoom
     * - https://www.google.com/maps/place/.../@lat,lng,zoom
     * - https://goo.gl/maps/... (shortened URLs - requires following redirect)
     * - https://maps.app.goo.gl/... (shortened URLs)
     */
    public double[] extractCoordinatesFromUrl(String url) {
        try {
            if (url == null || url.trim().isEmpty()) {
                throw new RuntimeException("URL cannot be empty");
            }

            // Handle shortened Google Maps URLs by following redirects
            String expandedUrl = url;
            if (url.contains("goo.gl") || url.contains("maps.app.goo.gl")) {
                expandedUrl = followRedirects(url);
            }

            // Pattern 1: @lat,lng format (most common in place URLs)
            java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
            java.util.regex.Matcher matcher1 = pattern1.matcher(expandedUrl);
            if (matcher1.find()) {
                double lat = Double.parseDouble(matcher1.group(1));
                double lng = Double.parseDouble(matcher1.group(2));
                return new double[]{lat, lng};
            }

            // Pattern 2: ?q=lat,lng format
            java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("[?&]q=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
            java.util.regex.Matcher matcher2 = pattern2.matcher(expandedUrl);
            if (matcher2.find()) {
                double lat = Double.parseDouble(matcher2.group(1));
                double lng = Double.parseDouble(matcher2.group(2));
                return new double[]{lat, lng};
            }

            // Pattern 3: ll=lat,lng format
            java.util.regex.Pattern pattern3 = java.util.regex.Pattern.compile("[?&]ll=(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
            java.util.regex.Matcher matcher3 = pattern3.matcher(expandedUrl);
            if (matcher3.find()) {
                double lat = Double.parseDouble(matcher3.group(1));
                double lng = Double.parseDouble(matcher3.group(2));
                return new double[]{lat, lng};
            }

            // Pattern 4: !3d lat !4d lng format (used in some embed URLs)
            java.util.regex.Pattern pattern4 = java.util.regex.Pattern.compile("!3d(-?\\d+\\.\\d+)!4d(-?\\d+\\.\\d+)");
            java.util.regex.Matcher matcher4 = pattern4.matcher(expandedUrl);
            if (matcher4.find()) {
                double lat = Double.parseDouble(matcher4.group(1));
                double lng = Double.parseDouble(matcher4.group(2));
                return new double[]{lat, lng};
            }

            throw new RuntimeException("Unable to extract coordinates from the provided URL. Please ensure it's a valid Google Maps URL.");

        } catch (NumberFormatException e) {
            log.error("Error parsing coordinates from URL: {}", url, e);
            throw new RuntimeException("Error parsing coordinates from URL");
        }
    }

    /**
     * Follow redirects for shortened URLs to get the final expanded URL
     */
    private String followRedirects(String shortenedUrl) {
        try {
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(shortenedUrl).openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("HEAD");
            connection.setRequestProperty("User-Agent", "TurfBookingApp/1.0");
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode >= 300 && responseCode < 400) {
                String redirectUrl = connection.getHeaderField("Location");
                if (redirectUrl != null) {
                    // Recursively follow redirects
                    return followRedirects(redirectUrl);
                }
            }
            return connection.getURL().toString();
        } catch (Exception e) {
            log.warn("Failed to follow redirects for URL: {}. Using original URL.", shortenedUrl, e);
            return shortenedUrl;
        }
    }
}

