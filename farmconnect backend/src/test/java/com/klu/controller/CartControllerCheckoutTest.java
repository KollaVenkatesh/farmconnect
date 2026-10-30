package com.klu.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.klu.model.CartItem;
import com.klu.model.PaymentOrder;
import com.klu.model.Product;
import com.klu.model.User;
import com.klu.repository.CartItemRepository;
import com.klu.repository.PaymentOrderRepository;
import com.klu.repository.ProductRepository;
import com.klu.repository.UserRepository;
import com.klu.service.EmailService;
import com.klu.service.RazorpayService;

class CartControllerCheckoutTest {

    @Test
    void verifyCheckout_shouldClearCartOnlyAfterCapturedPaymentIsVerified() {
        ProductRepository productRepository = mock(ProductRepository.class);
        CartItemRepository cartItemRepository = mock(CartItemRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        PaymentOrderRepository paymentOrderRepository = mock(PaymentOrderRepository.class);
        RazorpayService razorpayService = mock(RazorpayService.class);
        CartController controller = new CartController(cartItemRepository, productRepository, userRepository, paymentOrderRepository, razorpayService, mock(EmailService.class));

        CartItem item = new CartItem();
        item.setUserEmail("buyer@example.com");
        item.setProductId("demo-tomatoes");
        item.setQuantity(2);
        PaymentOrder order = new PaymentOrder();
        order.setBuyerEmail("buyer@example.com");
        order.setRazorpayOrderId("order_test123");
        order.setAmountFromPaise(9000L);
        order.setItems(List.of(Map.of("productId", "demo-tomatoes", "quantity", 2)));
        order.setStatus("PAYMENT_PENDING");
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("buyer@example.com");
        when(razorpayService.verifyPaymentSignature("order_test123", "pay_test123", "valid-signature")).thenReturn(true);
        when(paymentOrderRepository.findByRazorpayOrderIdAndBuyerEmail("order_test123", "buyer@example.com")).thenReturn(Optional.of(order));
        when(razorpayService.get("/v1/payments/pay_test123")).thenReturn(Map.of(
                "id", "pay_test123", "order_id", "order_test123", "amount", 9000, "currency", "INR", "status", "captured"));
        when(cartItemRepository.findByUserEmailAndProductId("buyer@example.com", "demo-tomatoes")).thenReturn(Optional.of(item));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = controller.verifyCheckout(Map.of(
                "razorpay_order_id", "order_test123",
                "razorpay_payment_id", "pay_test123",
                "razorpay_signature", "valid-signature"), auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("PAID", order.getStatus());
        assertEquals(true, order.getCartCleared());
        verify(cartItemRepository).delete(item);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UPI", "CARD", "NET_BANKING"})
    void checkout_shouldAcceptSupportedOnlinePaymentMethods(String paymentMethod) {
        ProductRepository productRepository = mock(ProductRepository.class);
        CartItemRepository cartItemRepository = mock(CartItemRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        PaymentOrderRepository paymentOrderRepository = mock(PaymentOrderRepository.class);
        RazorpayService razorpayService = mock(RazorpayService.class);
        CartController controller = new CartController(cartItemRepository, productRepository, userRepository, paymentOrderRepository, razorpayService, mock(EmailService.class));

        Product product = new Product();
        product.setProductId("demo-tomatoes");
        product.setProductName("Farm Fresh Tomatoes");
        product.setPrice(45.0);
        product.setUnit("kg");
        product.setStock(20);
        product.setFarmerId("farmer-1");

        CartItem item = new CartItem();
        item.setUserEmail("buyer@example.com");
        item.setProductId("demo-tomatoes");
        item.setQuantity(2);

        when(productRepository.findById("demo-tomatoes")).thenReturn(Optional.of(product));
        when(cartItemRepository.findAllByUserEmail("buyer@example.com")).thenReturn(List.of(item));
        User farmer = new User();
        farmer.setUserId("farmer-1");
        farmer.setRazorpayAccountId("acc_farmer1");
        farmer.setPayoutStatus("VERIFIED");
        when(userRepository.findById("farmer-1")).thenReturn(Optional.of(farmer));
        AtomicReference<Map<String, Object>> capturedOrderRequest = new AtomicReference<>();
        when(razorpayService.post(org.mockito.ArgumentMatchers.eq("/v1/orders"), anyMap(), isNull()))
            .thenAnswer(invocation -> {
                capturedOrderRequest.set(invocation.getArgument(1));
                return Map.of("id", "order_test123");
            });
        when(razorpayService.getKeyId()).thenReturn("rzp_test_public");
        when(paymentOrderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("buyer@example.com");

        ResponseEntity<?> response = controller.checkout(Map.of("paymentMethod", paymentMethod), auth);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals(paymentMethod, body.get("paymentMethod"));
        assertEquals("order_test123", body.get("orderId"));
        assertTrue(((Number) body.get("amount")).longValue() > 0);

        verify(razorpayService).post(eq("/v1/orders"), anyMap(), isNull());
        Map<String, Object> orderRequest = capturedOrderRequest.get();
        assertEquals(9000L, ((Number) orderRequest.get("amount")).longValue());
        List<?> transfers = (List<?>) orderRequest.get("transfers");
        assertEquals(1, transfers.size());
        Map<?, ?> transfer = (Map<?, ?>) transfers.get(0);
        assertEquals("acc_farmer1", transfer.get("account"));
        assertEquals(9000L, ((Number) transfer.get("amount")).longValue());
    }

    @Test
    void checkout_shouldPlaceCashOnDeliveryOrderWithDeliveryAddressAndClearCart() {
        ProductRepository productRepository = mock(ProductRepository.class);
        CartItemRepository cartItemRepository = mock(CartItemRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        PaymentOrderRepository paymentOrderRepository = mock(PaymentOrderRepository.class);
        RazorpayService razorpayService = mock(RazorpayService.class);
        EmailService emailService = mock(EmailService.class);
        CartController controller = new CartController(cartItemRepository, productRepository, userRepository, paymentOrderRepository, razorpayService, emailService);

        Product product = new Product();
        product.setProductId("demo-tomatoes");
        product.setProductName("Farm Fresh Tomatoes");
        product.setPrice(45.0);
        product.setStock(20);
        CartItem item = new CartItem();
        item.setUserEmail("buyer@example.com");
        item.setProductId("demo-tomatoes");
        item.setQuantity(2);

        when(cartItemRepository.findAllByUserEmail("buyer@example.com")).thenReturn(List.of(item));
        when(productRepository.findById("demo-tomatoes")).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserEmailAndProductId("buyer@example.com", "demo-tomatoes")).thenReturn(Optional.of(item));
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("buyer@example.com");

        ResponseEntity<?> response = controller.checkout(Map.of(
                "paymentMethod", "CASH_ON_DELIVERY",
                "deliveryAddress", "12 Farm Road, Chennai, 600001"), auth);

        assertEquals(200, response.getStatusCode().value());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertNotNull(body);
        assertEquals("COD_PENDING", body.get("status"));
        assertEquals("CASH_ON_DELIVERY", body.get("paymentMethod"));
        assertTrue(String.valueOf(body.get("orderId")).length() > 0);
        verify(razorpayService, org.mockito.Mockito.never()).post(org.mockito.ArgumentMatchers.anyString(), anyMap(), org.mockito.ArgumentMatchers.any());
        verify(cartItemRepository).delete(item);
        verify(emailService).sendOrderConfirmation(any(PaymentOrder.class), eq("buyer@example.com"));
    }
}
