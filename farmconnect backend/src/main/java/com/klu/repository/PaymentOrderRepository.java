package com.klu.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.klu.model.PaymentOrder;

public interface PaymentOrderRepository extends MongoRepository<PaymentOrder, String> {
    Optional<PaymentOrder> findByRazorpayOrderIdAndBuyerEmail(String razorpayOrderId, String buyerEmail);
    Optional<PaymentOrder> findByRazorpayOrderId(String razorpayOrderId);
    List<PaymentOrder> findByBuyerEmailOrderByCreatedAtDesc(String buyerEmail);
}
