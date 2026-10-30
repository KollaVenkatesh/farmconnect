package com.klu.service;

import java.math.BigDecimal;
import java.util.List;

import org.bson.BsonType;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.client.model.Filters;

/** Migrates legacy payment amounts to a two-decimal amountRupees field. */
@Component
public class PaymentOrderAmountMigration implements ApplicationRunner {
    private final MongoTemplate mongoTemplate;

    public PaymentOrderAmountMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        var collection = mongoTemplate.getCollection("payment_orders");
        Document amountInPaise = new Document("$toDecimal", "$amount");
        Document convertedPaiseAmount = new Document("$round", List.of(
                new Document("$divide", List.of(amountInPaise,
                        new Decimal128(BigDecimal.valueOf(100)))), 2));
        Document legacyFields = new Document("amountRupees", new Document("$ifNull", List.of(
                "$amountRupees", convertedPaiseAmount)))
                .append("amount", "$$REMOVE");
        var legacyResult = collection.updateMany(
                Filters.or(
                        Filters.type("amount", BsonType.INT64),
                        Filters.type("amount", BsonType.INT32)),
                List.of(new Document("$set", legacyFields)));

        Document interimFields = new Document("amountRupees", new Document("$ifNull", List.of(
                "$amountRupees", new Document("$round", List.of("$amount", 2)))))
                .append("amount", "$$REMOVE");
        var decimalResult = collection.updateMany(
                Filters.type("amount", BsonType.DECIMAL128),
                List.of(new Document("$set", interimFields)));

        Document normalizeRupees = new Document("$set", new Document(
                "amountRupees", new Document("$round", List.of("$amountRupees", 2)))
                .append("amount", "$$REMOVE"));
        var rupeesResult = collection.updateMany(
                Filters.type("amountRupees", BsonType.DECIMAL128), List.of(normalizeRupees));

        long convertedCount = legacyResult.getModifiedCount()
                + decimalResult.getModifiedCount() + rupeesResult.getModifiedCount();
        if (convertedCount > 0) {
            System.out.printf("Migrated %d payment amount records to amountRupees decimals.%n", convertedCount);
        }
    }
}
