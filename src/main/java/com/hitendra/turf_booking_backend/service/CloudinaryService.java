package com.hitendra.turf_booking_backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final ExecutorService executorService;

    /**
     * Constructor with dependency injection.
     * ExecutorService is injected to allow shared thread pool management.
     */
    @Autowired
    public CloudinaryService(Cloudinary cloudinary, ExecutorService imageUploadExecutor) {
        this.cloudinary = cloudinary;
        this.executorService = imageUploadExecutor;
    }

    /**
     * Shutdown executor service gracefully when application context closes.
     * This prevents resource leaks and allows JVM to terminate properly.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down CloudinaryService executor service");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("Executor service did not terminate in 30 seconds, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for executor service shutdown", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Upload a single image to Cloudinary
     * @param file The image file to upload
     * @return The secure URL of the uploaded image
     */
    public String uploadImage(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("File is empty or null");
            }

            log.info("Uploading image: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "turf_images",
                            "resource_type", "image"
                    ));

            String imageUrl = (String) uploadResult.get("secure_url");
            log.info("Image uploaded successfully: {}", imageUrl);
            return imageUrl;
        } catch (IOException e) {
            log.error("Error uploading image to Cloudinary: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error uploading image: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }

    /**
     * Upload multiple images to Cloudinary in parallel.
     * OPTIMIZED: Uses CompletableFuture for parallel uploads instead of sequential processing.
     * This significantly reduces total upload time when uploading multiple images.
     * 
     * @param files List of image files to upload
     * @return List of secure URLs of uploaded images
     */
    public List<String> uploadImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        // Filter out null/empty files first
        List<MultipartFile> validFiles = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .collect(Collectors.toList());

        if (validFiles.isEmpty()) {
            return new ArrayList<>();
        }

        log.info("Starting parallel upload of {} images", validFiles.size());

        // Create CompletableFuture for each upload with error tracking
        List<CompletableFuture<String>> uploadFutures = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        
        for (int i = 0; i < validFiles.size(); i++) {
            final int index = i;
            final MultipartFile file = validFiles.get(i);
            final String fileName = file.getOriginalFilename();
            fileNames.add(fileName);
            
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return uploadImage(file);
                } catch (Exception e) {
                    String errorMsg = String.format("Failed to upload image at index %d (%s): %s", 
                            index, fileName, e.getMessage());
                    log.error(errorMsg);
                    throw new RuntimeException(errorMsg, e);
                }
            }, executorService);
            
            uploadFutures.add(future);
        }

        // Wait for all uploads to complete and collect results
        try {
            CompletableFuture<Void> allUploads = CompletableFuture.allOf(
                    uploadFutures.toArray(new CompletableFuture[0])
            );

            // Wait for completion
            allUploads.join();

            // Collect all successful URLs
            List<String> imageUrls = uploadFutures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            log.info("Successfully uploaded {} images in parallel", imageUrls.size());
            return imageUrls;

        } catch (Exception e) {
            // Log and rethrow with simplified message
            throw new RuntimeException("Failed to upload one or more images. Check logs for details.", e);
        }
    }

    /**
     * Delete a single image from Cloudinary
     * @param imageUrl The URL of the image to delete
     */
    public void deleteImage(String imageUrl) {
        try {
            String publicId = extractPublicIdFromUrl(imageUrl);
            if (publicId != null) {
                Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Image deleted successfully: {} - Result: {}", publicId, result.get("result"));
            }
        } catch (IOException e) {
            log.error("Error deleting image from Cloudinary: {}", e.getMessage());
            // Don't throw exception here to avoid blocking the delete operation
        }
    }

    /**
     * Delete multiple images from Cloudinary
     * @param imageUrls List of image URLs to delete
     */
    public void deleteImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        for (String imageUrl : imageUrls) {
            deleteImage(imageUrl);
        }
    }

    /**
     * Extract public ID from Cloudinary URL
     * @param imageUrl The Cloudinary image URL
     * @return The public ID of the image
     */
    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            // URL format: https://res.cloudinary.com/{cloud_name}/image/upload/{version}/{public_id}.{format}
            if (imageUrl == null || !imageUrl.contains("cloudinary.com")) {
                return null;
            }

            String[] parts = imageUrl.split("/upload/");
            if (parts.length < 2) {
                return null;
            }

            String pathAfterUpload = parts[1];
            // Remove version if present (e.g., v1234567890/)
            String publicIdWithExtension = pathAfterUpload.replaceFirst("v\\d+/", "");

            // Remove file extension
            int lastDotIndex = publicIdWithExtension.lastIndexOf('.');
            if (lastDotIndex > 0) {
                return publicIdWithExtension.substring(0, lastDotIndex);
            }

            return publicIdWithExtension;
        } catch (Exception e) {
            log.error("Error extracting public ID from URL {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }
}
