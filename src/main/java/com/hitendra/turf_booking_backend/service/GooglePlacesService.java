package com.hitendra.turf_booking_backend.service;

import com.hitendra.turf_booking_backend.dto.service.GoogleRatingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Service for fetching ratings from Google Places API (New).
 *
 * Supports two operations:
 * 1. Fetch rating by place_id (used by scheduler)
 * 2. Search by name+city to auto-discover place_id and fetch rating (used on service creation)
 */
@Service
@Slf4j
public class GooglePlacesService {

    private static final String PLACES_API_URL = "https://places.googleapis.com/v1/places/";
    private static final String TEXT_SEARCH_URL = "https://places.googleapis.com/v1/places:searchText";

    private final RestTemplate restTemplate;
    private final String apiKey;

    public GooglePlacesService(
            RestTemplate restTemplate,
            @Value("${google.api.key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
    }

    /**
     * Search for a place using name + city and return its place_id, rating, and review count.
     * Uses the Places API (New) Text Search endpoint:
     *   POST https://places.googleapis.com/v1/places:searchText
     *
     * @param name the service/venue name
     * @param city the city name
     * @return GooglePlaceSearchResult with placeId, rating, and userRatingCount, or null if not found
     */
    public GooglePlaceSearchResult searchAndFetchRating(String name, String city) {
        if (name == null || name.isBlank()) {
            log.warn("searchAndFetchRating called with null or blank name");
            return null;
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.error("Google API key is not configured. Set google.api.key in application properties.");
            return null;
        }

        try {
            String textQuery = name + " " + (city != null ? city : "");

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Goog-Api-Key", apiKey);
            headers.set("X-Goog-FieldMask", "places.id,places.rating,places.userRatingCount");
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = Map.of("textQuery", textQuery.trim());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    TEXT_SEARCH_URL,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> places = (List<Map<String, Object>>) body.get("places");

                if (places != null && !places.isEmpty()) {
                    Map<String, Object> firstPlace = places.get(0);

                    String placeId = (String) firstPlace.get("id");
                    Double rating = firstPlace.get("rating") != null
                            ? ((Number) firstPlace.get("rating")).doubleValue() : null;
                    Integer userRatingCount = firstPlace.get("userRatingCount") != null
                            ? ((Number) firstPlace.get("userRatingCount")).intValue() : null;

                    log.info("Found Google Place for '{}': placeId={}, rating={}, reviewCount={}",
                            textQuery.trim(), placeId, rating, userRatingCount);

                    return new GooglePlaceSearchResult(placeId, rating, userRatingCount);
                }

                log.info("No Google Place found for query: '{}'", textQuery.trim());
                return null;
            }

            log.warn("Unexpected response status {} for text search query '{}'",
                    response.getStatusCode(), textQuery.trim());
            return null;

        } catch (HttpClientErrorException.TooManyRequests e) {
            log.error("Google Places API rate limit exceeded during text search for '{} {}'", name, city);
            return null;
        } catch (HttpClientErrorException e) {
            log.error("Google Places API client error during text search for '{} {}': {} - {}",
                    name, city, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (ResourceAccessException e) {
            log.error("Google Places API connection error during text search for '{} {}': {}",
                    name, city, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error searching Google Place for '{} {}': {}",
                    name, city, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Fetch rating and review count for a given Google Place ID.
     *
     * @param placeId the Google Places place_id
     * @return GoogleRatingResponse with rating and userRatingCount, or null if fetch fails
     */
    public GoogleRatingResponse fetchRating(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            log.warn("fetchRating called with null or blank placeId");
            return null;
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.error("Google API key is not configured. Set google.api.key in application properties.");
            return null;
        }

        try {
            String url = PLACES_API_URL + placeId;

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Goog-Api-Key", apiKey);
            headers.set("X-Goog-FieldMask", "rating,userRatingCount");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<GoogleRatingResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    GoogleRatingResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                GoogleRatingResponse body = response.getBody();
                log.debug("Fetched rating for placeId={}: rating={}, reviewCount={}",
                        placeId, body.getRating(), body.getUserRatingCount());
                return body;
            }

            log.warn("Unexpected response status {} for placeId={}", response.getStatusCode(), placeId);
            return null;

        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Google Place not found for placeId={}", placeId);
            return null;
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.error("Google Places API rate limit exceeded while fetching placeId={}", placeId);
            return null;
        } catch (HttpClientErrorException e) {
            log.error("Google Places API client error for placeId={}: {} - {}",
                    placeId, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (ResourceAccessException e) {
            log.error("Google Places API connection error for placeId={}: {}", placeId, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching Google rating for placeId={}: {}", placeId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Result of a Google Places text search, containing the discovered place_id
     * along with the rating data.
     */
    public record GooglePlaceSearchResult(String placeId, Double rating, Integer userRatingCount) {}
}

