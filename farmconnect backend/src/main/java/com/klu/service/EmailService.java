package com.klu.service;

import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.klu.model.Notification;
import com.klu.model.PaymentOrder;
import com.klu.model.Product;
import com.klu.repository.NotificationRepository;
import com.klu.repository.ProductRepository;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;
    private final ProductRepository productRepository;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${farmconnect.mail.from:notifications@farmconnect.com}")
    private String mailFrom;

    @Value("${farmconnect.mail.enabled:true}")
    private boolean mailEnabled;

    public EmailService(
            @Autowired(required = false) JavaMailSender mailSender,
            NotificationRepository notificationRepository,
            ProductRepository productRepository) {
        this.mailSender = mailSender;
        this.notificationRepository = notificationRepository;
        this.productRepository = productRepository;
    }

    /**
     * Sends Order Confirmation and Receipt to the buyer after successful payment.
     */
    public void sendOrderConfirmation(PaymentOrder order, String buyerName) {
        if (order == null || order.getBuyerEmail() == null) return;

        String buyerEmail = order.getBuyerEmail();
        String name = (buyerName != null && !buyerName.isBlank()) ? buyerName : buyerEmail.split("@")[0];
        String orderId = order.getId() != null ? order.getId() : order.getRazorpayOrderId();
        double totalRupees = order.getAmountRupees().doubleValue();
        String formattedDate = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(order.getCreatedAt() != null ? order.getCreatedAt() : new Date());
        boolean cashOnDelivery = "CASH_ON_DELIVERY".equals(order.getPaymentMethod());

        StringBuilder itemsTableRows = new StringBuilder();
        StringBuilder textItems = new StringBuilder();

        if (order.getItems() != null) {
            for (Map<String, Object> itemMap : order.getItems()) {
                String productId = String.valueOf(itemMap.get("productId"));
                int quantity = ((Number) itemMap.getOrDefault("quantity", 1)).intValue();
                Product product = productRepository.findById(productId).orElse(null);

                String productName = product != null ? product.getName() : "Farm Fresh Produce";
                String unit = product != null && product.getUnit() != null ? product.getUnit() : "unit";
                double price = product != null && product.getPrice() != null ? product.getPrice() : 0.0;
                double lineTotal = price * quantity;

                itemsTableRows.append("<tr>")
                        .append("<td style='padding: 10px; border-bottom: 1px solid #e5e7eb;'><strong>").append(escapeHtml(productName)).append("</strong></td>")
                        .append("<td style='padding: 10px; border-bottom: 1px solid #e5e7eb; text-align: center;'>").append(quantity).append(" ").append(escapeHtml(unit)).append("</td>")
                        .append("<td style='padding: 10px; border-bottom: 1px solid #e5e7eb; text-align: right;'>&#8377;").append(String.format("%.2f", price)).append("</td>")
                        .append("<td style='padding: 10px; border-bottom: 1px solid #e5e7eb; text-align: right; font-weight: bold;'>&#8377;").append(String.format("%.2f", lineTotal)).append("</td>")
                        .append("</tr>");

                textItems.append("- ").append(productName).append(" x ").append(quantity).append(" ")
                        .append(unit).append(" = Rs.").append(String.format("%.2f", lineTotal)).append("\n");
            }
        }

        String subject = "\uD83C\uDF3E FarmConnect Order Confirmed #" + orderId;
        String paymentStatus = cashOnDelivery ? "Order placed · Pay cash on delivery" : "Payment Received & Order Confirmed!";

        String html = "<!DOCTYPE html>"
                + "<html>"
                + "<head><meta charset='UTF-8'></head>"
                + "<body style='font-family: Arial, sans-serif; background-color: #f4f6f4; margin: 0; padding: 20px; color: #1f2937;'>"
                + "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08);'>"
                + "  <div style='background-color: #1e3a1f; color: #ffffff; padding: 24px; text-align: center;'>"
                + "    <h1 style='margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.5px;'>FarmConnect</h1>"
                + "    <p style='margin: 6px 0 0; font-size: 14px; opacity: 0.9;'>Good food, grown closer.</p>"
                + "  </div>"
                + "  <div style='padding: 28px;'>"
                + "    <div style='background-color: #ecfdf5; border-left: 4px solid #10b981; padding: 14px 16px; border-radius: 6px; margin-bottom: 24px;'>"
                + "      <p style='margin: 0; font-size: 15px; color: #065f46; font-weight: 600;'>&#10003; " + paymentStatus + "</p>"
                + "    </div>"
                + "    <p style='font-size: 15px; margin: 0 0 16px;'>Hello <strong>" + escapeHtml(name) + "</strong>,</p>"
                + "    <p style='font-size: 14px; line-height: 1.6; color: #4b5563; margin: 0 0 20px;'>"
                + "      Thank you for supporting local farmers! Your order has been placed successfully and the farmers have been notified to harvest and pack your fresh produce."
                + "    </p>"
                + "    <div style='background-color: #f9fafb; border-radius: 8px; padding: 16px; margin-bottom: 24px;'>"
                + "      <table style='width: 100%; font-size: 13px; color: #6b7280;'>"
                + "        <tr><td><strong>Order ID:</strong></td><td style='text-align: right; color: #111827;'>" + escapeHtml(orderId) + "</td></tr>"
                + "        <tr><td><strong>Date:</strong></td><td style='text-align: right; color: #111827;'>" + formattedDate + "</td></tr>"
                + "        <tr><td><strong>Payment Ref:</strong></td><td style='text-align: right; color: #111827;'>" + escapeHtml(order.getPaymentId() != null ? order.getPaymentId() : "Verified Online") + "</td></tr>"
                + "        <tr><td><strong>Payment Method:</strong></td><td style='text-align: right; color: #111827;'>" + escapeHtml(order.getPaymentMethod() != null ? order.getPaymentMethod() : "UPI / Razorpay") + "</td></tr>"
                + (cashOnDelivery ? "        <tr><td><strong>Delivery Address:</strong></td><td style='text-align: right; color: #111827;'>" + escapeHtml(order.getDeliveryAddress()) + "</td></tr>" : "")
                + "      </table>"
                + "    </div>"
                + "    <h3 style='margin: 0 0 12px; font-size: 16px; color: #111827;'>Order Summary</h3>"
                + "    <table style='width: 100%; border-collapse: collapse; font-size: 14px; margin-bottom: 20px;'>"
                + "      <thead>"
                + "        <tr style='background-color: #f3f4f6; color: #374151; font-weight: 600; text-align: left;'>"
                + "          <th style='padding: 10px;'>Item</th>"
                + "          <th style='padding: 10px; text-align: center;'>Qty</th>"
                + "          <th style='padding: 10px; text-align: right;'>Price</th>"
                + "          <th style='padding: 10px; text-align: right;'>Total</th>"
                + "        </tr>"
                + "      </thead>"
                + "      <tbody>"
                + itemsTableRows.toString()
                + "      </tbody>"
                + "      <tfoot>"
                + "        <tr>"
                + "          <td colspan='3' style='padding: 12px 10px; text-align: right; font-weight: bold; font-size: 16px;'>Grand Total:</td>"
                + "          <td style='padding: 12px 10px; text-align: right; font-weight: bold; font-size: 16px; color: #1e3a1f;'>&#8377;" + String.format("%.2f", totalRupees) + "</td>"
                + "        </tr>"
                + "      </tfoot>"
                + "    </table>"
                + "    <div style='background-color: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 14px; font-size: 13px; color: #166534; line-height: 1.5;'>"
                + "      <strong>What happens next?</strong> Local growers are preparing your harvest for direct delivery or local community pickup. You can view all produce details in your account."
                + "    </div>"
                + "  </div>"
                + "  <div style='background-color: #f9fafb; padding: 16px; text-align: center; font-size: 12px; color: #9ca3af; border-top: 1px solid #e5e7eb;'>"
                + "    FarmConnect Marketplace &bull; Empowering Sustainable Regional Agriculture"
                + "  </div>"
                + "</div>"
                + "</body></html>";

        String text = (cashOnDelivery ? "FarmConnect COD Order Placed!\n\n" : "FarmConnect Order Confirmed!\n\n")
                + "Hello " + name + ",\n\n"
                + (cashOnDelivery
                    ? "Your order #" + orderId + " has been placed. Please pay cash on delivery to:\n" + order.getDeliveryAddress() + "\n\n"
                    : "Your order #" + orderId + " has been placed and payment has been verified.\n\n")
                + "Order Summary:\n"
                + textItems.toString()
                + "\nGrand Total: Rs." + String.format("%.2f", totalRupees) + "\n\n"
                + "Thank you for supporting local farmers!\n"
                + "FarmConnect - Good food, grown closer.";

        deliver(buyerEmail, name, subject, html, text, "ORDER_CONFIRMATION");
    }

    /**
     * Sends alert to a farmer notifying them that items from their harvest have been ordered.
     */
    public void sendFarmerOrderAlert(String farmerEmail, String farmerName, PaymentOrder order, List<Map<String, Object>> farmerItems, double farmerPayout) {
        if (farmerEmail == null || farmerEmail.isBlank()) return;

        String name = (farmerName != null && !farmerName.isBlank()) ? farmerName : "Farmer";
        String orderId = order.getId() != null ? order.getId() : order.getRazorpayOrderId();
        boolean cashOnDelivery = "CASH_ON_DELIVERY".equals(order.getPaymentMethod());
        String subject = "\uD83D\uDE9C New FarmConnect Order Received #" + orderId;

        StringBuilder itemsList = new StringBuilder();
        StringBuilder textList = new StringBuilder();

        for (Map<String, Object> item : farmerItems) {
            String prodName = String.valueOf(item.getOrDefault("name", "Produce Item"));
            int qty = ((Number) item.getOrDefault("quantity", 1)).intValue();
            String unit = String.valueOf(item.getOrDefault("unit", "kg"));

            itemsList.append("<li style='margin-bottom: 6px;'><strong>").append(escapeHtml(prodName))
                    .append("</strong>: ").append(qty).append(" ").append(escapeHtml(unit)).append("</li>");

            textList.append("- ").append(prodName).append(": ").append(qty).append(" ").append(unit).append("\n");
        }

        String html = "<!DOCTYPE html><html><body style='font-family: Arial, sans-serif; background-color: #f4f6f4; margin: 0; padding: 20px; color: #1f2937;'>"
                + "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden;'>"
                + "  <div style='background-color: #1e3a1f; color: #ffffff; padding: 20px; text-align: center;'>"
                + "    <h2 style='margin:0;'>FarmConnect &bull; Harvest Order Alert</h2>"
                + "  </div>"
                + "  <div style='padding: 24px;'>"
                + "    <p>Dear <strong>" + escapeHtml(name) + "</strong>,</p>"
                + "    <p>Great news! A customer just placed an order for your produce through FarmConnect.</p>"
                + "    <div style='background-color: #f0fdf4; border-radius: 8px; padding: 14px; margin: 16px 0;'>"
                + "      <h4 style='margin: 0 0 10px; color: #166534;'>Items to prepare:</h4>"
                + "      <ul style='margin: 0; padding-left: 20px;'>" + itemsList.toString() + "</ul>"
                + "      <p style='margin: 12px 0 0; font-size: 14px;'><strong>" + (cashOnDelivery ? "Cash to collect:" : "Expected Payout:") + "</strong> &#8377;" + String.format("%.2f", farmerPayout) + "</p>"
                + "    </div>"
                + "    <p style='font-size: 13px; color: #6b7280;'>" + (cashOnDelivery
                    ? "Please prepare this harvest for delivery or pickup and collect payment from the buyer."
                    : "Please prepare this harvest for delivery or pickup. Razorpay transfers will be routed to your verified bank account.") + "</p>"
                + "  </div>"
                + "</div></body></html>";

        String text = "FarmConnect Harvest Order Alert\n\n"
                + "Dear " + name + ",\n\n"
                + "A customer has ordered your produce (Order #" + orderId + "):\n\n"
                + textList.toString()
                + (cashOnDelivery ? "\nCash to collect: Rs." : "\nExpected Payout: Rs.") + String.format("%.2f", farmerPayout) + "\n\n"
                + "Thank you,\nFarmConnect Team";

        deliver(farmerEmail, name, subject, html, text, "FARMER_NEW_ORDER");
    }

    /**
     * Sends a welcome email upon user registration.
     */
    public void sendWelcomeEmail(String email, String name, String role) {
        if (email == null || email.isBlank()) return;

        String displayName = (name != null && !name.isBlank()) ? name : email.split("@")[0];
        boolean isFarmer = "FARMER".equalsIgnoreCase(role);
        String subject = "\uD83C\uDF31 Welcome to FarmConnect, " + displayName + "!";

        String roleMessage = isFarmer
                ? "As a registered farmer, you can list fresh harvests, manage stock, set locations for community pickup, and receive automated payouts directly to your verified bank account."
                : "Explore seasonal crops grown by local farmers, auto-select ingredients for any recipe with our AI Cook Assistant, and support regional agriculture with secure payments.";

        String html = "<!DOCTYPE html><html><body style='font-family: Arial, sans-serif; background-color: #f4f6f4; margin: 0; padding: 20px; color: #1f2937;'>"
                + "<div style='max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden;'>"
                + "  <div style='background-color: #1e3a1f; color: #ffffff; padding: 24px; text-align: center;'>"
                + "    <h1 style='margin: 0; font-size: 24px;'>Welcome to FarmConnect!</h1>"
                + "    <p style='margin: 6px 0 0; font-size: 14px; opacity: 0.9;'>Good food, grown closer.</p>"
                + "  </div>"
                + "  <div style='padding: 28px;'>"
                + "    <p style='font-size: 16px;'>Hello <strong>" + escapeHtml(displayName) + "</strong>,</p>"
                + "    <p style='font-size: 14px; line-height: 1.6; color: #4b5563;'>"
                + "      We're thrilled to welcome you to FarmConnect as a <strong>" + escapeHtml(role) + "</strong>. "
                + roleMessage
                + "    </p>"
                + "    <div style='background-color: #f9fafb; border-radius: 8px; padding: 16px; margin: 20px 0;'>"
                + "      <h4 style='margin: 0 0 10px; color: #111827;'>Explore FarmConnect Features:</h4>"
                + "      <ul style='margin: 0; padding-left: 20px; font-size: 14px; color: #4b5563; line-height: 1.6;'>"
                + "        <li><strong>Shop the Harvest:</strong> Direct farm produce filtered by category and item.</li>"
                + "        <li><strong>AI Chef & Recipe Match:</strong> Search any recipe to instantly add matched ingredients to cart.</li>"
                + "        <li><strong>Local Direct Payments:</strong> Secure checkout powered by Razorpay.</li>"
                + "      </ul>"
                + "    </div>"
                + "    <p style='font-size: 14px; color: #4b5563;'>Visit <a href='http://localhost:5173' style='color: #1e3a1f; font-weight: bold;'>FarmConnect Marketplace</a> to get started!</p>"
                + "  </div>"
                + "  <div style='background-color: #f9fafb; padding: 16px; text-align: center; font-size: 12px; color: #9ca3af;'>"
                + "    FarmConnect &bull; Direct from Soil to Table"
                + "  </div>"
                + "</div></body></html>";

        String text = "Welcome to FarmConnect!\n\n"
                + "Hello " + displayName + ",\n\n"
                + "Welcome as a registered " + role + " on FarmConnect. " + roleMessage + "\n\n"
                + "Visit http://localhost:5173 to explore produce and features.\n\n"
                + "Best regards,\nThe FarmConnect Team";

        deliver(email, displayName, subject, html, text, "WELCOME_SIGNUP");
    }

    /**
     * Sends a test notification to verify email configuration.
     */
    public Notification sendTestNotification(String email, String name) {
        String displayName = (name != null && !name.isBlank()) ? name : "FarmConnect User";
        String subject = "\uD83D\uDD14 FarmConnect Email System Test Notification";
        String html = "<!DOCTYPE html><html><body style='font-family: Arial, sans-serif; padding: 20px;'>"
                + "<div style='max-width: 500px; margin: 0 auto; background: #ffffff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 20px;'>"
                + "  <h3 style='color: #1e3a1f; margin-top: 0;'>\uD83C\uDF3E FarmConnect Notification System Active</h3>"
                + "  <p>Hello " + escapeHtml(displayName) + ",</p>"
                + "  <p>This is a live test notification verifying that the FarmConnect email and notification engine is fully functional.</p>"
                + "  <p style='font-size: 12px; color: #6b7280;'>Timestamp: " + new Date() + "</p>"
                + "</div></body></html>";
        String text = "FarmConnect Notification System Active\n\nHello " + displayName + ",\nThis is a test notification confirming your email system is active.\nTimestamp: " + new Date();

        return deliver(email, displayName, subject, html, text, "TEST_NOTIFICATION");
    }

    /**
     * Core delivery engine: tries JavaMailSender if configured, logs gracefully, and persists to MongoDB.
     * If SMTP_MAIL_PASSWORD is not set, emails are stored in MongoDB (visible in notification bell UI).
     */
    private Notification deliver(String to, String recipientName, String subject, String htmlContent, String textContent, String type) {
        String status = "LOGGED";

        boolean smtpReady = mailEnabled
                && mailSender != null
                && mailUsername != null && !mailUsername.isBlank()
                && mailPassword != null && !mailPassword.isBlank();

        if (smtpReady) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(mailFrom.isBlank() ? mailUsername : mailFrom, "FarmConnect");
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(textContent, htmlContent);

                mailSender.send(message);
                status = "SENT";
                log.info("[EmailService] ✅ Successfully sent {} email → {}", type, to);
            } catch (Exception ex) {
                log.warn("[EmailService] ❌ SMTP failed for {} → {}: {}", type, to, ex.getMessage());
                log.warn("[EmailService]    Hint: Check Gmail App Password in application-secrets.properties");
                status = "FAILED_SMTP_FALLBACK";
            }
        } else {
            if (mailUsername == null || mailUsername.isBlank()) {
                log.info("[EmailService] ℹ️  SPRING_MAIL_USERNAME not set. Notification logged to DB for {} ({})", to, type);
            } else if (mailPassword == null || mailPassword.isBlank()) {
                log.warn("[EmailService] ⚠️  SPRING_MAIL_PASSWORD is empty for user '{}'. Email NOT sent ({}).", mailUsername, type);
                log.warn("[EmailService]    → Set a Gmail App Password in application-secrets.properties to enable real email delivery.");
            } else if (!mailEnabled) {
                log.info("[EmailService] ℹ️  Email sending is disabled (farmconnect.mail.enabled=false). Logged to DB.");
            }
            status = "LOGGED";
        }

        Notification notification = new Notification(to, recipientName, subject, htmlContent, textContent, type, status);
        try {
            return notificationRepository.save(notification);
        } catch (Exception ex) {
            log.error("[EmailService] Could not save notification to database: {}", ex.getMessage());
            return notification;
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
