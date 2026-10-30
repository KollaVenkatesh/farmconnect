package com.klu.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.klu.dto.FarmerPayoutRequest;
import com.klu.model.User;
import com.klu.repository.UserRepository;
import com.klu.service.RazorpayService;

class FarmerPayoutControllerTest {

    @Test
    void createPayoutAccount_shouldForwardBankDataToRazorpayAndPersistOnlyMaskedMetadata() {
        UserRepository users = mock(UserRepository.class);
        RazorpayService razorpay = mock(RazorpayService.class);
        FarmerPayoutController controller = new FarmerPayoutController(users, razorpay);
        User farmer = new User();
        farmer.setUserId("farmer-123");
        farmer.setName("Ravi Kumar");
        farmer.setEmail("ravi@example.com");
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("ravi@example.com");
        when(users.findByEmail("ravi@example.com")).thenReturn(Optional.of(farmer));
        AtomicReference<Map<String, Object>> capturedProviderRequest = new AtomicReference<>();
        when(razorpay.post(eq("/v2/accounts"), anyMap(), anyString())).thenAnswer(invocation -> {
            capturedProviderRequest.set(invocation.getArgument(1));
            return Map.of("id", "acc_farmer123", "product_config", Map.of("activation_status", "activated", "active_configuration", Map.of(
                    "settlement_accounts", List.of(Map.of("verification_status", "pending", "bank_account", Map.of("account_number", "XXXXXXXXXXXXXX89"))))));
        });
        FarmerPayoutRequest request = new FarmerPayoutRequest();
        request.setLegalBusinessName("Ravi Kumar");
        request.setPhone("+919876543210");
        request.setPan("ABCDE1234F");
        request.setBeneficiaryName("Ravi Kumar");
        request.setAccountNumber("1234567890123456");
        request.setIfsc("HDFC0000317");
        request.setTermsAccepted(true);

        ResponseEntity<?> response = controller.createPayoutAccount(request, auth);

        assertEquals(201, response.getStatusCode().value());
        assertEquals("acc_farmer123", farmer.getRazorpayAccountId());
        assertEquals("PENDING", farmer.getPayoutStatus());
        assertEquals("89", farmer.getPayoutAccountLastDigits());
        verify(razorpay).post(eq("/v2/accounts"), anyMap(), anyString());
        Map<String, Object> providerRequest = capturedProviderRequest.get();
        Map<?, ?> legalInfo = (Map<?, ?>) providerRequest.get("legal_info");
        assertEquals("ABCDE1234F", legalInfo.get("pan"));
        List<?> settlements = (List<?>) providerRequest.get("settlement_accounts");
        Map<?, ?> bank = (Map<?, ?>) ((Map<?, ?>) settlements.get(0)).get("bank_account");
        assertEquals("1234567890123456", bank.get("account_number"));
        assertNotNull(response.getBody());
        assertFalse(farmer.getRazorpayAccountId().contains("1234567890123456"));
    }

    @Test
    void createPayoutAccount_shouldRejectInvalidIfscBeforeCallingRazorpay() {
        UserRepository users = mock(UserRepository.class);
        RazorpayService razorpay = mock(RazorpayService.class);
        FarmerPayoutController controller = new FarmerPayoutController(users, razorpay);
        User farmer = new User();
        farmer.setUserId("farmer-123");
        farmer.setEmail("ravi@example.com");
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("ravi@example.com");
        when(users.findByEmail("ravi@example.com")).thenReturn(Optional.of(farmer));
        FarmerPayoutRequest request = new FarmerPayoutRequest();
        request.setLegalBusinessName("Ravi Kumar");
        request.setPhone("+919876543210");
        request.setPan("ABCDE1234F");
        request.setBeneficiaryName("Ravi Kumar");
        request.setAccountNumber("1234567890123456");
        request.setIfsc("INVALID");
        request.setTermsAccepted(true);

        ResponseEntity<?> response = controller.createPayoutAccount(request, auth);

        assertEquals(400, response.getStatusCode().value());
        verify(razorpay, never()).post(eq("/v2/accounts"), anyMap(), anyString());
    }
}