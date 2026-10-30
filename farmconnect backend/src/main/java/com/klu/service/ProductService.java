package com.klu.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.klu.model.Product;
import com.klu.repository.ProductRepository;

@Service
public class ProductService {

    private static final Set<String> CATEGORIES = Set.of(
            "Vegetables", "Fruits", "Grains", "Nuts", "Pulses", "Spices", "Herbs", "Dairy");
    private static final Set<String> UNITS = Set.of(
            "kg", "grams", "tons", "piece", "dozen", "bunch", "bag");

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product addProduct(Product product) {
        normalizeAndValidate(product);
        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Product getProductById(String productId) {
        return productRepository.findById(productId).orElse(null);
    }

    public Product updateProduct(String productId, Product product) {
        normalizeAndValidate(product);
        Product existingProduct = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        existingProduct.setProductName(product.getProductName());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setStock(product.getStock());
        existingProduct.setImageUrl(product.getImageUrl());
        existingProduct.setRating(product.getRating());
        existingProduct.setStatus(product.getStatus());
        existingProduct.setUnit(product.getUnit());
        existingProduct.setLocation(product.getLocation());
        existingProduct.setFarmer(product.getFarmer());

        return productRepository.save(existingProduct);
    }

    public void deleteProduct(String productId) {
        if (!productRepository.existsById(productId)) {
            throw new RuntimeException("Product not found");
        }
        productRepository.deleteById(productId);
    }

    private void normalizeAndValidate(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product details are required.");
        }
        if (product.getProductName() == null || product.getProductName().isBlank()) {
            throw new IllegalArgumentException("Product name is required.");
        }
        product.setProductName(product.getProductName().trim());

        String category = canonicalValue(product.getCategory(), CATEGORIES);
        if (category == null) {
            throw new IllegalArgumentException("Choose a valid product category.");
        }
        product.setCategory(category);

        if (product.getPrice() == null || !Double.isFinite(product.getPrice()) || product.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be greater than zero.");
        }
        if (product.getStock() == null || product.getStock() < 0) {
            throw new IllegalArgumentException("Stock must be zero or greater.");
        }

        String unit = canonicalValue(product.getUnit(), UNITS);
        if (unit == null) {
            throw new IllegalArgumentException("Choose a valid unit.");
        }
        product.setUnit(unit);

        if (product.getLocation() == null || product.getLocation().isBlank()) {
            throw new IllegalArgumentException("Farm location is required.");
        }
        product.setLocation(product.getLocation().trim());
        if (product.getDescription() != null) {
            product.setDescription(product.getDescription().trim());
        }
    }

    private String canonicalValue(String value, Set<String> allowedValues) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return allowedValues.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst()
                .orElse(null);
    }
}

