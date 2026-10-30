package com.klu.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class RazorpayService {
    private static final ParameterizedTypeReference<Map<String, Object>> RESPONSE_TYPE = new ParameterizedTypeReference<>() {};

    private final String keyId;
    private final String keySecret;
    private final String webhookSecret;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public RazorpayService(RazorpayProperties properties) {
        if (properties != null) {
            this.keyId = properties.getKeyId();
            this.keySecret = properties.getKeySecret();
            this.webhookSecret = properties.getWebhookSecret();
        } else {
            this.keyId = "";
            this.keySecret = "";
            this.webhookSecret = "";
        }
    }

    public RazorpayService() {
        this.keyId = "";
        this.keySecret = "";
        this.webhookSecret = "";
    }

    public String getKeyId() {
        requireConfigured();
        return keyId;
    }

    public Map<String, Object> post(String path, Map<String, Object> body, String idempotencyKey) {
        requireConfigured();
        return client().post()
                .uri(path)
                .headers(headers -> {
                    headers.setBasicAuth(keyId, keySecret);
                    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                    if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                        headers.set("Idempotency-Key", idempotencyKey);
                    }
                })
                .body(body)
                .retrieve()
                .body(RESPONSE_TYPE);
    }

    public Map<String, Object> get(String path) {
        requireConfigured();
        return client().get()
                .uri(path)
                .headers(headers -> headers.setBasicAuth(keyId, keySecret))
                .retrieve()
                .body(RESPONSE_TYPE);
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        return verifySignature(orderId + "|" + paymentId, signature, keySecret);
    }

    public boolean verifyWebhookSignature(String rawBody, String signature) {
        return verifySignature(rawBody, signature, webhookSecret);
    }

    private boolean verifySignature(String value, String signature, String secret) {
        if (value == null || signature == null || secret == null || secret.isBlank()) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8))).getBytes(StandardCharsets.US_ASCII);
            return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception ex) {
            return false;
        }
    }

    private RestClient client() {
        return RestClient.builder().baseUrl("https://api.razorpay.com").build();
    }

    private void requireConfigured() {
        if (keyId == null || keyId.isBlank() || keySecret == null || keySecret.isBlank()) {
            throw new IllegalStateException("Razorpay is not configured on the server");
        }
    }
}
