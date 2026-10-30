package com.klu.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class RazorpayServiceTest {

    @Test
    void verifyPaymentSignature_shouldAcceptValidAndRejectTamperedSignatures() throws Exception {
        RazorpayService service = service();
        String signature = sign("order_abc|pay_123", "payment-secret");

        assertTrue(service.verifyPaymentSignature("order_abc", "pay_123", signature));
        assertFalse(service.verifyPaymentSignature("order_abc", "pay_other", signature));
    }

    @Test
    void verifyWebhookSignature_shouldUseSeparateWebhookSecret() throws Exception {
        RazorpayService service = service();
        String rawBody = "{\"event\":\"payment.captured\"}";

        assertTrue(service.verifyWebhookSignature(rawBody, sign(rawBody, "webhook-secret")));
        assertFalse(service.verifyWebhookSignature(rawBody, sign(rawBody, "payment-secret")));
    }

    private String sign(String value, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private RazorpayService service() {
        RazorpayProperties properties = new RazorpayProperties();
        properties.setKeyId("rzp_test_key");
        properties.setKeySecret("payment-secret");
        properties.setWebhookSecret("webhook-secret");
        return new RazorpayService(properties);
    }
}