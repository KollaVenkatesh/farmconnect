package com.klu.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klu.model.Product;
import com.klu.repository.UserRepository;
import com.klu.service.ProductService;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    public ProductController(ProductService productService, UserRepository userRepository) {
        this.productService = productService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<?> addProduct(@RequestBody Product product, Authentication authentication) {
        if (authentication != null) {
            userRepository.findByEmail(authentication.getName()).ifPresent(user -> {
                product.setFarmer(user.getName());
                product.setFarmerId(user.getUserId());
            });
        }
        try {
            Product savedProduct = productService.addProduct(product);
            return ResponseEntity.ok(savedProduct);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping
    public List<Product> getAllProducts(Authentication authentication) {
        List<Product> products = productService.getAllProducts();
        products.forEach(product -> product.setOwnedByCurrentUser(isOwner(authentication, product)));
        return products;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> getProductById(@PathVariable String productId) {
        Product product = productService.getProductById(productId);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    @PutMapping("/{productId}")
    public ResponseEntity<?> updateProduct(@PathVariable String productId, @RequestBody Product product, Authentication authentication) {
        Product existingProduct = productService.getProductById(productId);
        if (existingProduct == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        boolean isOwner = isOwner(authentication, existingProduct);
        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!isAdmin) {
            product.setFarmer(existingProduct.getFarmer());
            product.setFarmerId(existingProduct.getFarmerId());
        }
        try {
            return ResponseEntity.ok(productService.updateProduct(productId, product));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<String> deleteProduct(@PathVariable String productId, Authentication authentication) {
        Product existingProduct = productService.getProductById(productId);
        if (existingProduct == null) {
            return ResponseEntity.notFound().build();
        }

        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        boolean isOwner = isOwner(authentication, existingProduct);
        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        productService.deleteProduct(productId);
        return ResponseEntity.ok("Product deleted successfully");
    }

    private boolean isOwner(Authentication authentication, Product product) {
        if (authentication == null) {
            return false;
        }
        return userRepository.findByEmail(authentication.getName())
                .map(user -> product.getFarmerId() != null
                        ? Objects.equals(user.getUserId(), product.getFarmerId())
                        : user.getName().equals(product.getFarmer()))
                .orElse(false);
    }
}
