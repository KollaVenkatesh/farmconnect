package com.klu.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klu.model.PaymentOrder;
import com.klu.repository.CartItemRepository;
import com.klu.repository.PaymentOrderRepository;
import com.klu.service.RazorpayService;

@RestController
@RequestMapping("/api/payments/razorpay")
public class RazorpayWebhookController {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RazorpayService razorpay;
    private final PaymentOrderRepository paymentOrders;
    private final CartItemRepository carts;

    public RazorpayWebhookController(RazorpayService razorpay, PaymentOrderRepository paymentOrders, CartItemRepository carts) {
        this.razorpay = razorpay;
        this.paymentOrders = paymentOrders;
        this.carts = carts;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receive(
            @RequestBody String rawBody,
            @RequestHeader(name = "X-Razorpay-Signature", required = false) String signature) {
        if (!razorpay.verifyWebhookSignature(rawBody, signature)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            Map<String, Object> event = objectMapper.readValue(rawBody, new TypeReference<>() {});
            String eventName = String.valueOf(event.get("event"));
            Map<String, Object> payload = map(event.get("payload"));
            if ("payment.captured".equals(eventName)) {
                Map<String, Object> paymentWrapper = map(payload.get("payment"));
                Map<String, Object> payment = map(paymentWrapper.get("entity"));
                updateCapturedPayment(payment);
            } else if ("transfer.processed".equals(eventName) || "transfer.failed".equals(eventName)) {
                Map<String, Object> transferWrapper = map(payload.get("transfer"));
                Map<String, Object> transfer = map(transferWrapper.get("entity"));
                updateTransfer(String.valueOf(transfer.get("source")), eventName);
            }
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    private void updateCapturedPayment(Map<String, Object> payment) {
        String orderId = String.valueOf(payment.get("order_id"));
        paymentOrders.findByRazorpayOrderId(orderId).ifPresent(order -> {
            if (!(payment.get("amount") instanceof Number amount)
                    || amount.longValue() != order.amountInPaise()
                    || !"INR".equals(payment.get("currency"))) return;
            order.setPaymentId(String.valueOf(payment.get("id")));
            order.setStatus("PAID");
            paymentOrders.save(order);
            removePurchasedItems(order);
        });
    }

    private void updateTransfer(String orderId, String eventName) {
        paymentOrders.findByRazorpayOrderId(orderId).ifPresent(order -> {
            if ("transfer.failed".equals(eventName)) {
                order.setTransferStatus("FAILED");
            } else if (!"FAILED".equals(order.getTransferStatus())) {
                order.setTransferStatus("PROCESSING");
            }
            paymentOrders.save(order);
        });
    }

    private void removePurchasedItems(PaymentOrder order) {
        if (Boolean.TRUE.equals(order.getCartCleared()) || order.getItems() == null) return;
        for (Map<String, Object> purchased : order.getItems()) {
            String productId = String.valueOf(purchased.get("productId"));
            int purchasedQuantity = ((Number) purchased.get("quantity")).intValue();
            carts.findByUserEmailAndProductId(order.getBuyerEmail(), productId).ifPresent(current -> {
                if (current.getQuantity() <= purchasedQuantity) {
                    carts.delete(current);
                } else {
                    current.setQuantity(current.getQuantity() - purchasedQuantity);
                    carts.save(current);
                }
            });
        }
        order.setCartCleared(true);
        paymentOrders.save(order);
    }

    private Map<String, Object> map(Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> source) {
            source.forEach((key, item) -> {
                if (key instanceof String name) result.put(name, item);
            });
        }
        return result;
    }
}
