package com.klu.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import com.klu.model.*;
import com.klu.repository.*;
import com.klu.service.RazorpayService;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartItemRepository carts;
    private final ProductRepository products;
    private final UserRepository users;
    private final PaymentOrderRepository paymentOrders;
    private final RazorpayService razorpay;
    private final com.klu.service.EmailService emailService;

    public CartController(CartItemRepository carts, ProductRepository products, UserRepository users,
            PaymentOrderRepository paymentOrders, RazorpayService razorpay,
            com.klu.service.EmailService emailService) {
        this.carts = carts;
        this.products = products;
        this.users = users;
        this.paymentOrders = paymentOrders;
        this.razorpay = razorpay;
        this.emailService = emailService;
    }

    @GetMapping
    public Map<String, Object> getCart(Authentication auth) { return cart(auth.getName()); }

    @GetMapping("/orders")
    public List<PaymentOrder> getMyOrders(Authentication auth) {
        return paymentOrders.findByBuyerEmailOrderByCreatedAtDesc(auth.getName());
    }

    @PostMapping
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            String productId = String.valueOf(body.get("productId"));
            if (products.findById(productId).isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Product not found"));
            CartItem item = carts.findByUserEmailAndProductId(auth.getName(), productId).orElseGet(() -> { CartItem fresh = new CartItem(); fresh.setUserEmail(auth.getName()); fresh.setProductId(productId); fresh.setQuantity(0); return fresh; });
            item.setQuantity(item.getQuantity() + 1); carts.save(item);
            return ResponseEntity.ok(cart(auth.getName()));
        } catch (RuntimeException ex) { return ResponseEntity.badRequest().body(Map.of("error", "A valid productId is required")); }
    }

    @PostMapping("/batch")
    public ResponseEntity<?> addBatchToCart(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Object itemsObj = body.get("items");
            if (itemsObj instanceof List<?> list) {
                for (Object entry : list) {
                    if (entry instanceof Map<?, ?> itemMap) {
                        String productId = String.valueOf(itemMap.get("productId"));
                        int quantity = 1;
                        if (itemMap.get("quantity") instanceof Number num) {
                            quantity = Math.max(1, num.intValue());
                        }
                        if (products.existsById(productId)) {
                            CartItem item = carts.findByUserEmailAndProductId(auth.getName(), productId).orElseGet(() -> {
                                CartItem fresh = new CartItem();
                                fresh.setUserEmail(auth.getName());
                                fresh.setProductId(productId);
                                fresh.setQuantity(0);
                                return fresh;
                            });
                            item.setQuantity(item.getQuantity() + quantity);
                            carts.save(item);
                        }
                    }
                }
            } else if (body.get("productIds") instanceof List<?> idList) {
                for (Object idObj : idList) {
                    String productId = String.valueOf(idObj);
                    if (products.existsById(productId)) {
                        CartItem item = carts.findByUserEmailAndProductId(auth.getName(), productId).orElseGet(() -> {
                            CartItem fresh = new CartItem();
                            fresh.setUserEmail(auth.getName());
                            fresh.setProductId(productId);
                            fresh.setQuantity(0);
                            return fresh;
                        });
                        item.setQuantity(item.getQuantity() + 1);
                        carts.save(item);
                    }
                }
            }
            return ResponseEntity.ok(cart(auth.getName()));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to add items to cart: " + ex.getMessage()));
        }
    }

    @PutMapping("/{productId}")
    public ResponseEntity<?> updateQuantity(@PathVariable String productId, @RequestBody Map<String, Object> body, Authentication auth) {
        Object requestedQuantity = body.get("quantity");
        if (!(requestedQuantity instanceof Number number) || number.intValue() < 1) {
            return ResponseEntity.badRequest().body(Map.of("error", "Quantity must be at least 1"));
        }

        Product product = products.findById(productId).orElse(null);
        if (product == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Product not found"));
        }
        int quantity = number.intValue();
        if (product.getStock() != null && quantity > product.getStock()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Quantity exceeds available stock"));
        }

        CartItem item = carts.findByUserEmailAndProductId(auth.getName(), productId).orElse(null);
        if (item == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Item is not in your cart"));
        }
        item.setQuantity(quantity);
        carts.save(item);
        return ResponseEntity.ok(cart(auth.getName()));
    }

    @DeleteMapping("/{productId}")
    public Map<String, Object> removeFromCart(@PathVariable String productId, Authentication auth) {
        carts.findByUserEmailAndProductId(auth.getName(), productId).ifPresent(carts::delete);
        return cart(auth.getName());
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody Map<String, Object> body, Authentication auth) {
        List<CartItem> cartRows = carts.findAllByUserEmail(auth.getName());
        if (cartRows.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Your cart is empty"));
        }

        String paymentMethod = body == null || body.get("paymentMethod") == null
                ? "UPI"
                : String.valueOf(body.get("paymentMethod")).trim();

        String deliveryAddress = body == null || body.get("deliveryAddress") == null
                ? ""
                : String.valueOf(body.get("deliveryAddress")).trim();
        Set<String> allowedPaymentMethods = Set.of("UPI", "CARD", "NET_BANKING", "CASH_ON_DELIVERY");
        if (!allowedPaymentMethods.contains(paymentMethod)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Choose a supported payment method"));
        }
        if ("CASH_ON_DELIVERY".equals(paymentMethod) && (deliveryAddress.isBlank() || deliveryAddress.length() > 500)) {
            return ResponseEntity.badRequest().body(Map.of("error", "A delivery address of up to 500 characters is required for Cash on Delivery"));
        }

        try {
            Map<String, SellerTransfer> transfersByFarmer = new LinkedHashMap<>();
            List<Map<String, Object>> orderItems = new ArrayList<>();
            long totalAmount = 0;

            for (CartItem row : cartRows) {
                Product product = products.findById(row.getProductId()).orElse(null);
                if (product == null) return ResponseEntity.badRequest().body(Map.of("error", "A product in your cart is no longer available"));
                if (row.getQuantity() == null || row.getQuantity() < 1 || (product.getStock() != null && row.getQuantity() > product.getStock())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "A cart quantity exceeds the available stock"));
                }
                if (product.getPrice() == null || product.getPrice() <= 0) {
                    return ResponseEntity.badRequest().body(Map.of("error", "A product in your cart has an invalid price"));
                }

                long lineAmount = BigDecimal.valueOf(product.getPrice())
                        .multiply(BigDecimal.valueOf(row.getQuantity()))
                        .setScale(2, RoundingMode.HALF_UP)
                        .movePointRight(2)
                        .longValueExact();
                totalAmount = Math.addExact(totalAmount, lineAmount);

                if (product.getFarmerId() != null && !product.getFarmerId().isBlank()) {
                    User farmer = users.findById(product.getFarmerId()).orElse(null);
                    if (farmer != null && farmer.getRazorpayAccountId() != null && "VERIFIED".equalsIgnoreCase(farmer.getPayoutStatus())) {
                        transfersByFarmer.computeIfAbsent(farmer.getUserId(), ignored -> new SellerTransfer(farmer))
                                .amount += lineAmount;
                    }
                }
                orderItems.add(Map.of("productId", row.getProductId(), "quantity", row.getQuantity()));
            }

            if (totalAmount <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "The cart total must be greater than zero"));
            }

            if ("CASH_ON_DELIVERY".equals(paymentMethod)) {
                PaymentOrder order = new PaymentOrder();
                order.setId(UUID.randomUUID().toString());
                order.setBuyerEmail(auth.getName());
                order.setAmountFromPaise(totalAmount);
                order.setItems(orderItems);
                order.setPaymentMethod(paymentMethod);
                order.setDeliveryAddress(deliveryAddress);
                order.setStatus("COD_PENDING");
                order.setTransferStatus("MANUAL_COD");
                paymentOrders.save(order);
                removePurchasedItems(order);
                try {
                    emailService.sendOrderConfirmation(order, auth.getName());
                    notifyFarmersForOrder(order);
                } catch (RuntimeException ex) {
                    System.err.println("Could not send Cash on Delivery order notifications: " + ex.getMessage());
                }
                return ResponseEntity.ok(Map.of(
                        "orderId", order.getId(),
                        "status", order.getStatus(),
                        "paymentMethod", paymentMethod,
                        "message", "Order placed. Please pay cash on delivery."
                ));
            }

            if (totalAmount < 100) {
                return ResponseEntity.badRequest().body(Map.of("error", "The minimum online payment amount is ₹1.00"));
            }

            List<Map<String, Object>> transfers = new ArrayList<>();
            for (SellerTransfer transfer : transfersByFarmer.values()) {
                if (transfer.amount >= 100) {
                    transfers.add(Map.of(
                            "account", transfer.farmer.getRazorpayAccountId(),
                            "amount", transfer.amount,
                            "currency", "INR",
                            "notes", Map.of("farmconnect_farmer_id", transfer.farmer.getUserId())
                    ));
                }
            }

            String localOrderId = UUID.randomUUID().toString();
            Map<String, Object> providerRequest = new LinkedHashMap<>();
            providerRequest.put("amount", totalAmount);
            providerRequest.put("currency", "INR");
            providerRequest.put("receipt", "FC-" + localOrderId.replace("-", ""));
            if (!transfers.isEmpty()) {
                providerRequest.put("transfers", transfers);
            }
            providerRequest.put("notes", Map.of("farmconnect_order_id", localOrderId));

            Map<String, Object> providerOrder;
            try {
                providerOrder = razorpay.post("/v1/orders", providerRequest, null);
            } catch (RestClientResponseException ex) {
                if (!transfers.isEmpty()) {
                    providerRequest.remove("transfers");
                    providerOrder = razorpay.post("/v1/orders", providerRequest, null);
                } else {
                    throw ex;
                }
            }

            PaymentOrder order = new PaymentOrder();
            order.setId(localOrderId);
            order.setBuyerEmail(auth.getName());
            order.setRazorpayOrderId(String.valueOf(providerOrder.get("id")));
            order.setAmountFromPaise(totalAmount);
            order.setItems(orderItems);
            order.setPaymentMethod(paymentMethod);
            order.setDeliveryAddress(deliveryAddress.isBlank() ? null : deliveryAddress);
            order.setStatus("PAYMENT_PENDING");
            order.setTransferStatus(transfers.isEmpty() ? "DIRECT" : "CREATED");
            paymentOrders.save(order);

            return ResponseEntity.ok(Map.of(
                    "keyId", razorpay.getKeyId(),
                    "orderId", order.getRazorpayOrderId(),
                    "amount", totalAmount,
                    "currency", "INR",
                    "paymentMethod", paymentMethod,
                    "status", order.getStatus()
            ));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "Razorpay is not configured on the server"));
        } catch (RestClientResponseException ex) {
            System.err.println("Razorpay Error: " + ex.getResponseBodyAsString());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Razorpay error: " + ex.getResponseBodyAsString()));
        } catch (ArithmeticException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "The cart total is outside the supported payment range"));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Payment setup failed. Please try again."));
        }
    }

    @PostMapping("/checkout/verify")
    public ResponseEntity<?> verifyCheckout(@RequestBody Map<String, Object> body, Authentication auth) {
        String orderId = body == null ? "" : String.valueOf(body.getOrDefault("razorpay_order_id", ""));
        String paymentId = body == null ? "" : String.valueOf(body.getOrDefault("razorpay_payment_id", ""));
        String signature = body == null ? "" : String.valueOf(body.getOrDefault("razorpay_signature", ""));
        if (!orderId.matches("^order_[A-Za-z0-9]+$") || !paymentId.matches("^pay_[A-Za-z0-9]+$")
                || !razorpay.verifyPaymentSignature(orderId, paymentId, signature)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Payment signature could not be verified"));
        }

        PaymentOrder order = paymentOrders.findByRazorpayOrderIdAndBuyerEmail(orderId, auth.getName()).orElse(null);
        if (order == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Payment order was not found"));
        if ("PAID".equals(order.getStatus())) {
            removePurchasedItems(order);
            return ResponseEntity.ok(Map.of("status", "PAID", "message", "Payment verified and farmer transfers are processing"));
        }

        try {
            Map<String, Object> payment = razorpay.get("/v1/payments/" + paymentId);
            if (!orderId.equals(payment.get("order_id")) || !(payment.get("amount") instanceof Number amount)
                    || amount.longValue() != order.amountInPaise() || !"INR".equals(payment.get("currency"))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Payment details do not match this order"));
            }
            if (!"captured".equals(payment.get("status"))) {
                return ResponseEntity.accepted().body(Map.of("status", "PAYMENT_PENDING", "message", "Payment is still processing"));
            }

            order.setPaymentId(paymentId);
            order.setStatus("PAID");
            paymentOrders.save(order);
            removePurchasedItems(order);

            // Trigger Email Notifications to buyer and farmers
            try {
                emailService.sendOrderConfirmation(order, auth.getName());
                notifyFarmersForOrder(order);
            } catch (Exception ex) {
                // Email notifications run asynchronously in background; don't fail payment verification
            }

            return ResponseEntity.ok(Map.of("status", "PAID", "message", "Payment verified. Farmer transfers are processing."));
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Could not verify payment with Razorpay. Please retry shortly."));
        }
    }

    private void removePurchasedItems(PaymentOrder order) {
        if (Boolean.TRUE.equals(order.getCartCleared())) return;
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

    private void notifyFarmersForOrder(PaymentOrder order) {
        if (order == null || order.getItems() == null) return;
        Map<String, List<Map<String, Object>>> itemsByFarmer = new HashMap<>();
        Map<String, Double> payoutByFarmer = new HashMap<>();

        for (Map<String, Object> itemMap : order.getItems()) {
            String productId = String.valueOf(itemMap.get("productId"));
            Product product = products.findById(productId).orElse(null);
            if (product != null && product.getFarmerId() != null && !product.getFarmerId().isBlank()) {
                int quantity = ((Number) itemMap.getOrDefault("quantity", 1)).intValue();
                double lineTotal = (product.getPrice() != null ? product.getPrice() : 0.0) * quantity;
                Map<String, Object> details = new HashMap<>();
                details.put("name", product.getName());
                details.put("quantity", quantity);
                details.put("unit", product.getUnit());
                details.put("lineTotal", lineTotal);

                itemsByFarmer.computeIfAbsent(product.getFarmerId(), k -> new ArrayList<>()).add(details);
                payoutByFarmer.put(product.getFarmerId(), payoutByFarmer.getOrDefault(product.getFarmerId(), 0.0) + lineTotal);
            }
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : itemsByFarmer.entrySet()) {
            User farmer = users.findById(entry.getKey()).orElse(null);
            if (farmer != null && farmer.getEmail() != null) {
                emailService.sendFarmerOrderAlert(farmer.getEmail(), farmer.getName(), order, entry.getValue(), payoutByFarmer.getOrDefault(entry.getKey(), 0.0));
            }
        }
    }

    private static final class SellerTransfer {
        private final User farmer;
        private long amount;

        private SellerTransfer(User farmer) {
            this.farmer = farmer;
        }
    }

    private Map<String, Object> cart(String email) {
        List<Map<String, Object>> items = new ArrayList<>(); int count = 0; double total = 0;
        for (CartItem row : carts.findAllByUserEmail(email)) {
            Product p = products.findById(row.getProductId()).orElse(null); if (p == null) continue;
            count += row.getQuantity();
            double unitPrice = p.getPrice() == null ? 0 : p.getPrice();
            total += unitPrice * row.getQuantity();
            Map<String, Object> item = new LinkedHashMap<>(); item.put("productId", p.getId()); item.put("name", p.getName()); item.put("quantity", row.getQuantity()); item.put("unitPrice", unitPrice); item.put("unit", p.getUnit()); item.put("stock", p.getStock()); items.add(item);
        }
        return Map.of("count", count, "items", items, "total", total);
    }
}
