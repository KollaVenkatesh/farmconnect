package com.klu.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;

import com.klu.dto.FarmerPayoutRequest;
import com.klu.model.User;
import com.klu.repository.UserRepository;
import com.klu.service.RazorpayService;

@RestController
@RequestMapping("/api/farmer/payout-account")
public class FarmerPayoutController {
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final Pattern IFSC_PATTERN = Pattern.compile("^[A-Z]{4}0[A-Z0-9]{6}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{8,15}$");
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[0-9]{5,20}$");

    private final UserRepository users;
    private final RazorpayService razorpay;

    public FarmerPayoutController(UserRepository users, RazorpayService razorpay) {
        this.users = users;
        this.razorpay = razorpay;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getPayoutAccount(Authentication auth) {
        User user = users.findByEmail(auth.getName()).orElse(null);
        if (user == null || user.getRazorpayAccountId() == null) {
            return ResponseEntity.ok(Map.of("configured", false, "status", "NOT_CONFIGURED"));
        }

        try {
            Map<String, Object> account = razorpay.get("/v2/accounts/" + user.getRazorpayAccountId());
            Map<String, Object> payout = payoutSummary(account);
            user.setPayoutStatus(String.valueOf(payout.get("status")));
            user.setPayoutAccountLastDigits(String.valueOf(payout.getOrDefault("accountEnding", "")));
            users.save(user);
            return ResponseEntity.ok(payout);
        } catch (RuntimeException ex) {
            return ResponseEntity.ok(storedSummary(user));
        }
    }

    @PostMapping
    public ResponseEntity<?> createPayoutAccount(@RequestBody FarmerPayoutRequest request, Authentication auth) {
        User user = users.findByEmail(auth.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Farmer account was not found"));
        }
        if (user.getRazorpayAccountId() != null && !user.getRazorpayAccountId().isBlank()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "A payout account is already registered. Contact support to change it."));
        }
        String validationError = validate(request);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(Map.of("error", validationError));
        }

        Map<String, Object> bankAccount = new LinkedHashMap<>();
        bankAccount.put("account_number", request.getAccountNumber().trim());
        bankAccount.put("beneficiary_name", request.getBeneficiaryName().trim());
        bankAccount.put("code_type", "ifsc");
        bankAccount.put("code", request.getIfsc().trim().toUpperCase());
        bankAccount.put("currency", "INR");
        bankAccount.put("is_default", true);

        Map<String, Object> settlement = Map.of("method", "bank_account", "bank_account", bankAccount);
        Map<String, Object> legalInfo = Map.of("pan", request.getPan().trim().toUpperCase());
        Map<String, Object> providerRequest = new LinkedHashMap<>();
        providerRequest.put("type", "route");
        providerRequest.put("tnc_accepted", true);
        providerRequest.put("reference_id", user.getUserId());
        providerRequest.put("legal_business_name", request.getLegalBusinessName().trim());
        providerRequest.put("business_type", "individual");
        providerRequest.put("email", user.getEmail());
        providerRequest.put("phone", request.getPhone().trim());
        providerRequest.put("legal_info", legalInfo);
        providerRequest.put("settlement_accounts", List.of(settlement));
        providerRequest.put("notes", Map.of("farmconnect_user_id", user.getUserId()));

        try {
            Map<String, Object> account = razorpay.post("/v2/accounts", providerRequest, UUID.randomUUID().toString());
            String accountId = String.valueOf(account.get("id"));
            if (accountId.isBlank() || "null".equals(accountId)) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Razorpay did not return a payout account"));
            }
            Map<String, Object> payout = payoutSummary(account);
            user.setRazorpayAccountId(accountId);
            user.setPayoutStatus(String.valueOf(payout.get("status")));
            user.setPayoutAccountLastDigits(String.valueOf(payout.getOrDefault("accountEnding", "")));
            users.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(payout);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "Razorpay payout setup is not configured on the server"));
        } catch (RestClientResponseException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Razorpay could not verify these payout details. Check them and try again."));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Payout setup could not be completed. Please try again."));
        }
    }

    private String validate(FarmerPayoutRequest request) {
        if (request == null) return "Enter your payout details";
        String legalName = trim(request.getLegalBusinessName());
        String phone = trim(request.getPhone());
        String pan = trim(request.getPan()).toUpperCase();
        String beneficiary = trim(request.getBeneficiaryName());
        String account = trim(request.getAccountNumber());
        String ifsc = trim(request.getIfsc()).toUpperCase();
        if (legalName.length() < 4 || legalName.length() > 200) return "Enter your legal name as shown on your PAN";
        if (!PHONE_PATTERN.matcher(phone).matches()) return "Enter a valid phone number including country code if needed";
        if (!PAN_PATTERN.matcher(pan).matches()) return "Enter a valid 10-character PAN";
        if (beneficiary.isBlank() || beneficiary.length() > 100) return "Enter the bank account holder name";
        if (!ACCOUNT_PATTERN.matcher(account).matches()) return "Enter a valid bank account number";
        if (!IFSC_PATTERN.matcher(ifsc).matches()) return "Enter a valid 11-character IFSC code";
        if (!Boolean.TRUE.equals(request.getTermsAccepted())) return "Accept Razorpay's linked-account terms to continue";
        return null;
    }

    private Map<String, Object> payoutSummary(Map<String, Object> account) {
        Map<String, Object> product = map(account.get("product_config"));
        Map<String, Object> activeConfiguration = map(product.get("active_configuration"));
        List<?> settlements = activeConfiguration.get("settlement_accounts") instanceof List<?> list ? list : List.of();
        Map<String, Object> settlement = settlements.isEmpty() ? Map.of() : map(settlements.get(0));
        Map<String, Object> bank = map(settlement.get("bank_account"));
        String status = String.valueOf(settlement.getOrDefault("verification_status", product.getOrDefault("activation_status", "pending"))).toUpperCase();
        String accountNumber = String.valueOf(bank.getOrDefault("account_number", ""));
        String ending = accountNumber.length() >= 2 ? accountNumber.substring(accountNumber.length() - 2) : "";
        return Map.of("configured", true, "status", status, "accountEnding", ending);
    }

    private Map<String, Object> storedSummary(User user) {
        return Map.of("configured", true,
                "status", user.getPayoutStatus() == null ? "PENDING" : user.getPayoutStatus().toUpperCase(),
                "accountEnding", user.getPayoutAccountLastDigits() == null ? "" : user.getPayoutAccountLastDigits());
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

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}