package com.hitendra.turf_booking_backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.hitendra.turf_booking_backend.entity.Booking;
import com.hitendra.turf_booking_backend.entity.Invoice;
import com.hitendra.turf_booking_backend.entity.InvoiceTemplate;
import com.hitendra.turf_booking_backend.entity.User;
import com.hitendra.turf_booking_backend.exception.InvoiceException;
import com.hitendra.turf_booking_backend.repository.InvoiceRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Service for generating and managing invoices.
 *
 * RESPONSIBILITIES:
 * 1. Fetch active template from cache (via InvoiceTemplateService)
 * 2. Generate HTML from template string using Thymeleaf
 * 3. Convert HTML to PDF using OpenHTMLtoPDF
 * 4. Upload PDF to Cloudinary
 * 5. Store invoice metadata in database with template version
 * 6. Ensure idempotency (no duplicate invoices)
 *
 * ARCHITECTURE:
 * - Template is fetched from cache (not file system)
 * - Template content is stored in PostgreSQL
 * - Invoice generation is asynchronous (triggered by BookingConfirmedEvent)
 */
@Service
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceTemplateService templateService;
    private final Cloudinary cloudinary;

    /**
     * Constructor with Cloudinary as optional dependency.
     * If Cloudinary is not configured, invoice generation will fail with clear error message.
     */
    @Autowired
    public InvoiceService(
            InvoiceRepository invoiceRepository,
            InvoiceTemplateService templateService,
            @Autowired(required = false) Cloudinary cloudinary) {
        this.invoiceRepository = invoiceRepository;
        this.templateService = templateService;
        this.cloudinary = cloudinary;

        if (cloudinary == null) {
            log.warn("⚠️  Cloudinary is not configured. Invoice generation will fail until Cloudinary is properly configured.");
        } else {
            log.info("✅ InvoiceService initialized with Cloudinary support");
        }
    }

    // GST rate constant (18%)
    private static final double GST_RATE = 18.0;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Verify Cloudinary is configured.
     * Throws exception if Cloudinary bean is null.
     */
    private void verifyCloudinaryConfigured() {
        if (cloudinary == null) {
            throw new InvoiceException(
                "Cloudinary is not configured. Please set environment variables: " +
                "CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET"
            );
        }
    }
    /**
     * Create a Thymeleaf TemplateEngine for processing string-based templates.
     * This is needed because template content comes from database, not files.
     */
    private TemplateEngine createStringTemplateEngine() {
        StringTemplateResolver templateResolver = new StringTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);

        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        return templateEngine;
    }

    /**
     * Generate and store invoice for a booking.
     *
     * IDEMPOTENCY:
     * - Checks if invoice already exists for bookingId
     * - Returns existing URL if found
     *
     * FLOW:
     * 1. Check if invoice exists (idempotency)
     * 2. Fetch active template from cache
     * 3. Prepare Thymeleaf context with booking data
     * 4. Process template string to HTML
     * 5. Convert HTML to PDF
     * 6. Upload PDF to Cloudinary
     * 7. Save invoice entity with template version
     * 8. Return secure URL
     *
     * @param booking Confirmed booking
     * @return Cloudinary secure URL of the invoice PDF
     */
    @Transactional
    public String generateAndStoreInvoice(Booking booking) {
        log.info("🧾 Generating invoice for booking ID: {}", booking.getId());

        // ═══════════════════════════════════════════════════════════════════════
        // STEP 0: Verify Cloudinary is configured
        // ═══════════════════════════════════════════════════════════════════════
        verifyCloudinaryConfigured();

        // ═══════════════════════════════════════════════════════════════════════
        // IDEMPOTENCY: Check if invoice already exists
        // ═══════════════════════════════════════════════════════════════════════
        if (invoiceRepository.existsByBookingId(booking.getId())) {
            Invoice existingInvoice = invoiceRepository.findByBookingId(booking.getId())
                    .orElseThrow(() -> new InvoiceException("Invoice exists but could not be retrieved"));
            log.info("✅ Invoice already exists for booking ID: {}. Returning existing URL (idempotent).", booking.getId());
            return existingInvoice.getCloudinaryUrl();
        }

        try {
            // ═══════════════════════════════════════════════════════════════════════
            // STEP 1: Fetch active template from cache
            // ═══════════════════════════════════════════════════════════════════════
            InvoiceTemplate activeTemplate = templateService.getActiveTemplate();
            log.info("📄 Using template: Version={}, Name={}", activeTemplate.getVersion(), activeTemplate.getName());

            // ═══════════════════════════════════════════════════════════════════════
            // STEP 2: Prepare Thymeleaf context
            // ═══════════════════════════════════════════════════════════════════════
            Context context = prepareInvoiceContext(booking);

            // ═══════════════════════════════════════════════════════════════════════
            // STEP 3: Process template STRING to HTML
            // ═══════════════════════════════════════════════════════════════════════
            TemplateEngine stringTemplateEngine = createStringTemplateEngine();
            String htmlContent = stringTemplateEngine.process(activeTemplate.getContent(), context);
            log.info("✅ HTML template processed for booking ID: {}", booking.getId());

            // ═══════════════════════════════════════════════════════════════════════
            // STEP 4: Convert HTML to PDF
            // ═══════════════════════════════════════════════════════════════════════
            byte[] pdfBytes = convertHtmlToPdf(htmlContent);
            log.info("✅ PDF generated for booking ID: {} (size: {} bytes)", booking.getId(), pdfBytes.length);

            // ═══════════════════════════════════════════════════════════════════════
            // STEP 5: Upload PDF to Cloudinary
            // ═══════════════════════════════════════════════════════════════════════
            String invoiceNumber = "INV-" + booking.getId();
            Map<String, Object> uploadResult = uploadPdfToCloudinary(pdfBytes, invoiceNumber);
            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            log.info("✅ PDF uploaded to Cloudinary for booking ID: {}. URL: {}", booking.getId(), secureUrl);

            // ═══════════════════════════════════════════════════════════════════════
            // STEP 6: Save invoice entity with template version
            // ═══════════════════════════════════════════════════════════════════════
            Invoice invoice = Invoice.builder()
                    .bookingId(booking.getId())
                    .invoiceNumber(invoiceNumber)
                    .cloudinaryUrl(secureUrl)
                    .cloudinaryPublicId(publicId)
                    .invoiceAmount(booking.getAmount())
                    .templateVersion(activeTemplate.getVersion()) // Track which template was used
                    .createdAt(LocalDateTime.now())
                    .build();

            invoiceRepository.save(invoice);
            log.info("✅ Invoice saved to database: Invoice ID: {}, Booking ID: {}, Template Version: {}",
                    invoice.getId(), booking.getId(), invoice.getTemplateVersion());

            return secureUrl;

        } catch (Exception e) {
            log.error("❌ Failed to generate invoice for booking ID: {}", booking.getId(), e);
            throw new InvoiceException("Failed to generate invoice: " + e.getMessage());
        }
    }

    /**
     * Prepare Thymeleaf context with booking data.
     */
    private Context prepareInvoiceContext(Booking booking) {
        Context context = new Context();

        com.hitendra.turf_booking_backend.entity.Service service = booking.getService();
        User customer = booking.getUser();

        // Venue/Issuer Information
        context.setVariable("venueName", service.getName());
        context.setVariable("venueGstin", service.getGstin() != null ? service.getGstin() : "N/A");
        context.setVariable("venueAddress", service.getLocation() != null ? service.getLocation() : "N/A");
        context.setVariable("venueState", service.getState() != null ? service.getState() : "N/A");
        context.setVariable("placeOfSupply", service.getState() != null ? service.getState() : "N/A");

        // Customer Information
        String customerName = customer != null ? customer.getName() : "Walk-in Customer";
        String customerAddress = customer != null && customer.getAddress() != null
            ? customer.getAddress()
            : "N/A";
        String customerGstin = customer != null && customer.getGstin() != null
            ? customer.getGstin()
            : "N/A";

        context.setVariable("customerName", customerName);
        context.setVariable("customerAddress", customerAddress);
        context.setVariable("customerGstin", customerGstin);

        // Invoice Meta Information
        context.setVariable("bookingId", booking.getId());
        context.setVariable("invoiceNumber", "INV-" + booking.getId());
        context.setVariable("invoiceDate", LocalDateTime.now().format(DATE_FORMATTER));
        context.setVariable("sacCode", "998599"); // SAC code for sports and recreation services
        context.setVariable("category", booking.getActivityCode() != null ? booking.getActivityCode() : "BOOKING");

        // Service Description
        String serviceDescription = String.format("%s - %s on %s from %s to %s",
            booking.getActivityCode() != null ? booking.getActivityCode() : "Service",
            booking.getResource() != null ? booking.getResource().getName() : service.getName(),
            booking.getBookingDate().format(DATE_FORMATTER),
            booking.getStartTime().format(TIME_FORMATTER),
            booking.getEndTime().format(TIME_FORMATTER));

        context.setVariable("serviceDescription", serviceDescription);
        context.setVariable("refundTerms", "As per venue cancellation policy");

        // Pricing Calculation (assuming amount is without GST)
        double bookingAmount = booking.getAmount();
        double discount = 0.0; // Can be extended to support discounts
        double netAmount = bookingAmount - discount;
        double gstAmount = (netAmount * GST_RATE) / 100.0;
        double invoiceTotal = netAmount + gstAmount;

        // Invoice Table Data
        context.setVariable("quantity", 1);
        context.setVariable("unitPrice", String.format("%.2f", bookingAmount));
        context.setVariable("amount", String.format("%.2f", bookingAmount));
        context.setVariable("discount", String.format("%.2f", discount));
        context.setVariable("netAmount", String.format("%.2f", netAmount));

        // Tax Section
        context.setVariable("gstAmount", String.format("%.2f", gstAmount));
        context.setVariable("invoiceTotal", String.format("%.2f", invoiceTotal));

        // Amount in Words
        context.setVariable("invoiceTotalWords", convertAmountToWords(invoiceTotal));

        return context;
    }

    /**
     * Convert HTML to PDF using OpenHTMLtoPDF.
     */
    private byte[] convertHtmlToPdf(String htmlContent) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        }
    }

    /**
     * Upload PDF to Cloudinary with resource_type = "raw".
     */
    private Map<String, Object> uploadPdfToCloudinary(byte[] pdfBytes, String invoiceNumber) throws IOException {
        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "resource_type", "raw",
                    "folder", "invoices",
                    "public_id", invoiceNumber,
                    "format", "pdf"
            );

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    pdfBytes,
                    uploadParams
            );

            log.info("Invoice uploaded to Cloudinary: {}", uploadResult.get("secure_url"));
            return uploadResult;

        } catch (IOException e) {
            log.error("Failed to upload invoice to Cloudinary: {}", e.getMessage(), e);
            throw new IOException("Cloudinary upload failed: " + e.getMessage());
        }
    }

    /**
     * Convert amount to words (Indian numbering system).
     * Simplified implementation.
     */
    private String convertAmountToWords(double amount) {
        String[] units = {"", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine"};
        String[] teens = {"Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
        String[] tens = {"", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"};

        try {
            int rupees = (int) amount;
            int paise = (int) Math.round((amount - rupees) * 100);

            if (rupees == 0 && paise == 0) {
                return "Zero Rupees Only";
            }

            StringBuilder words = new StringBuilder();

            // Handle thousands
            if (rupees >= 1000) {
                int thousands = rupees / 1000;
                words.append(convertHundreds(thousands, units, teens, tens)).append(" Thousand ");
                rupees %= 1000;
            }

            // Handle hundreds
            if (rupees >= 100) {
                int hundreds = rupees / 100;
                words.append(units[hundreds]).append(" Hundred ");
                rupees %= 100;
            }

            // Handle tens and units
            if (rupees >= 20) {
                words.append(tens[rupees / 10]).append(" ");
                rupees %= 10;
            } else if (rupees >= 10) {
                words.append(teens[rupees - 10]).append(" ");
                rupees = 0;
            }

            if (rupees > 0) {
                words.append(units[rupees]).append(" ");
            }

            words.append("Rupees");

            if (paise > 0) {
                words.append(" and ").append(paise).append(" Paise");
            }

            words.append(" Only");

            return words.toString().trim();

        } catch (Exception e) {
            log.error("Error converting amount to words: {}", e.getMessage());
            return "Amount: ₹" + String.format("%.2f", amount);
        }
    }

    private String convertHundreds(int num, String[] units, String[] teens, String[] tens) {
        StringBuilder result = new StringBuilder();

        if (num >= 100) {
            result.append(units[num / 100]).append(" Hundred ");
            num %= 100;
        }

        if (num >= 20) {
            result.append(tens[num / 10]).append(" ");
            num %= 10;
        } else if (num >= 10) {
            result.append(teens[num - 10]).append(" ");
            return result.toString();
        }

        if (num > 0) {
            result.append(units[num]).append(" ");
        }

        return result.toString().trim();
    }

    /**
     * Get invoice by booking ID.
     */
    @Transactional(readOnly = true)
    public Invoice getInvoiceByBookingId(Long bookingId) {
        return invoiceRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new InvoiceException("Invoice not found for booking ID: " + bookingId));
    }
}

