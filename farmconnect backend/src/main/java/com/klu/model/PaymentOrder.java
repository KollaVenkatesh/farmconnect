package com.klu.model;

import java.util.List;
import java.util.Map;
import java.util.Date;
import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("payment_orders")
public class PaymentOrder {
    @Id private String id;
    private String buyerEmail;
    private String razorpayOrderId;
    private String paymentId;
    private String status;
    private String transferStatus;
    private Boolean cartCleared;
    /** Stored as a decimal INR value, for example 250.00. */
    private BigDecimal amountRupees;
    private List<Map<String, Object>> items;
    private String paymentMethod;
    private Date createdAt = new Date();
    private String deliveryAddress;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBuyerEmail() { return buyerEmail; }
    public void setBuyerEmail(String buyerEmail) { this.buyerEmail = buyerEmail; }
    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTransferStatus() { return transferStatus; }
    public void setTransferStatus(String transferStatus) { this.transferStatus = transferStatus; }
    public Boolean getCartCleared() { return cartCleared; }
    public void setCartCleared(Boolean cartCleared) { this.cartCleared = cartCleared; }
    public BigDecimal getAmountRupees() { return amountRupees; }
    public void setAmountRupees(BigDecimal amountRupees) {
        this.amountRupees = amountRupees == null ? null
                : amountRupees.setScale(2, java.math.RoundingMode.UNNECESSARY);
    }
    /** Convert the persisted rupee amount to Razorpay's integer paise amount. */
    public long amountInPaise() { return amountRupees.movePointRight(2).longValueExact(); }
    public void setAmountFromPaise(long paise) {
        this.amountRupees = BigDecimal.valueOf(paise, 2).setScale(2);
    }
    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
}
