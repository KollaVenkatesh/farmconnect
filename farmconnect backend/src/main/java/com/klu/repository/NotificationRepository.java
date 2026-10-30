package com.klu.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.klu.model.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);

    long countByRecipientEmailAndReadFalse(String recipientEmail);
}
