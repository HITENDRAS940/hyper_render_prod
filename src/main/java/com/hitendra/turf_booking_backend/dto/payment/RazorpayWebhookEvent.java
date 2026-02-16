package com.hitendra.turf_booking_backend.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayWebhookEvent {
    private String entity;
    private String account_id;
    private String event;
    private Boolean contains;
    private Long created_at;
    private WebhookPayload payload;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookPayload {
        private WebhookPayment payment;
        private WebhookOrder order;
        private WebhookRefund refund;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookPayment {
        private Entity entity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookOrder {
        private Entity entity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WebhookRefund {
        private RefundEntity entity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entity {
        private String id;
        private String entity;
        private Integer amount;
        private String currency;
        private String status;
        private String order_id;
        private String invoice_id;
        private Boolean international;
        private String method;
        private Integer amount_refunded;
        private String refund_status;
        private Boolean captured;
        private String description;
        private String card_id;
        private String bank;
        private String wallet;
        private String vpa;
        private String email;
        private String contact;
        private Long created_at;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundEntity {
        private String id;
        private String entity;
        private Integer amount;
        private String currency;
        private String payment_id;
        private String status;
        private String speed_requested;
        private String speed_processed;
        private Long created_at;
        private java.util.Map<String, Object> notes;
    }
}

