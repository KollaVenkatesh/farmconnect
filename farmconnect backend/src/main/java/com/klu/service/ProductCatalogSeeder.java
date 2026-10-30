package com.klu.service;

import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.klu.model.Product;
import com.klu.model.Role;
import com.klu.model.User;
import com.klu.repository.ProductRepository;
import com.klu.repository.UserRepository;

@Component
public class ProductCatalogSeeder implements CommandLineRunner {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProductCatalogSeeder(ProductRepository productRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Seed default demo accounts
        if (!userRepository.existsByEmail("buyer@farmconnect.com")) {
            User buyer = new User();
            buyer.setName("Demo Buyer");
            buyer.setUsername("buyer@farmconnect.com");
            buyer.setEmail("buyer@farmconnect.com");
            buyer.setPassword(passwordEncoder.encode("password123"));
            buyer.setRole(Role.USER);
            userRepository.save(buyer);
        }

        if (!userRepository.existsByEmail("farmer@farmconnect.com")) {
            User farmer = new User();
            farmer.setName("Meera's Garden");
            farmer.setUsername("farmer@farmconnect.com");
            farmer.setEmail("farmer@farmconnect.com");
            farmer.setPassword(passwordEncoder.encode("password123"));
            farmer.setRole(Role.FARMER);
            userRepository.save(farmer);
        }

        List<Product> catalog = List.of(
            product("demo-tomatoes", "Farm Fresh Tomatoes", "Vegetables", "Vine-ripened seasonal tomatoes.", 45, 80, "kg", "Meera's Garden", "Chennai, Tamil Nadu", 4.8),
            product("demo-green-chillies", "Green Chillies", "Vegetables", "Fresh green chillies picked this week.", 60, 60, "kg", "Meera's Garden", "Chennai, Tamil Nadu", 4.6),
            product("demo-onions", "Red Onions", "Vegetables", "Firm, flavorful onions for everyday cooking.", 38, 90, "kg", "Kaveri Farm", "Erode, Tamil Nadu", 4.6),
            product("demo-potatoes", "Farm Potatoes", "Vegetables", "Freshly dug, versatile table potatoes.", 32, 100, "kg", "Green Field Farm", "Thanjavur, Tamil Nadu", 4.5),
            product("demo-carrots", "Orange Carrots", "Vegetables", "Sweet and crisp carrots, freshly harvested.", 55, 55, "kg", "Meera's Garden", "Chennai, Tamil Nadu", 4.7),
            product("demo-cabbage", "Green Cabbage", "Vegetables", "Compact, leafy cabbage grown with care.", 28, 60, "kg", "Hillview Growers", "Nilgiris, Tamil Nadu", 4.5),
            product("demo-cauliflower", "Cauliflower", "Vegetables", "Fresh white florets from a local farm.", 40, 45, "piece", "Hillview Growers", "Nilgiris, Tamil Nadu", 4.6),
            product("demo-brinjal", "Purple Brinjal", "Vegetables", "Tender, glossy brinjal for curries and roasts.", 42, 50, "kg", "Kaveri Farm", "Erode, Tamil Nadu", 4.5),
            product("demo-ladies-finger", "Ladies Finger", "Vegetables", "Young, tender okra picked at its best.", 48, 40, "kg", "Meera's Garden", "Chennai, Tamil Nadu", 4.6),
            product("demo-spinach", "Fresh Spinach", "Vegetables", "Leafy greens harvested fresh for the week.", 25, 35, "bunch", "Meera's Garden", "Chennai, Tamil Nadu", 4.8),
            product("demo-cucumber", "Green Cucumber", "Vegetables", "Cool, crisp cucumbers for salads and meals.", 30, 60, "kg", "Kaveri Farm", "Erode, Tamil Nadu", 4.5),
            product("demo-pumpkin", "Sweet Pumpkin", "Vegetables", "Naturally sweet pumpkin from local fields.", 35, 50, "kg", "Green Field Farm", "Thanjavur, Tamil Nadu", 4.6),
            product("demo-beans", "Green Beans", "Vegetables", "Fresh, crunchy beans picked this morning.", 52, 45, "kg", "Hillview Growers", "Nilgiris, Tamil Nadu", 4.6),
            product("demo-mangoes", "Alphonso Mangoes", "Fruits", "Sweet, ripe mangoes from the summer harvest.", 120, 45, "kg", "Sunrise Orchards", "Salem, Tamil Nadu", 4.9),
            product("demo-banana", "Hill Bananas", "Fruits", "Naturally ripened bananas from the hills.", 50, 70, "dozen", "Sunrise Orchards", "Salem, Tamil Nadu", 4.7),
            product("demo-papaya", "Ripe Papaya", "Fruits", "Sweet, tree-ripened papaya from local orchards.", 45, 40, "kg", "Sunrise Orchards", "Salem, Tamil Nadu", 4.6),
            product("demo-guava", "Fresh Guava", "Fruits", "Aromatic guavas picked at peak ripeness.", 65, 35, "kg", "Anand Family Farm", "Coimbatore, Tamil Nadu", 4.7),
            product("demo-watermelon", "Watermelon", "Fruits", "Juicy seasonal watermelon from the farm.", 30, 50, "kg", "Kaveri Farm", "Erode, Tamil Nadu", 4.5),
            product("demo-pomegranate", "Pomegranate", "Fruits", "Ruby-red, sweet-tart pomegranates.", 160, 25, "kg", "Sunrise Orchards", "Salem, Tamil Nadu", 4.8),
            product("demo-grapes", "Black Grapes", "Fruits", "Sweet grapes from local growers.", 95, 30, "kg", "Anand Family Farm", "Coimbatore, Tamil Nadu", 4.6),
            product("demo-rice", "Native Red Rice", "Grains", "Nutty, locally harvested red rice.", 80, 100, "kg", "Green Field Farm", "Thanjavur, Tamil Nadu", 4.7),
            product("demo-millet", "Foxtail Millet", "Grains", "Wholesome local millet, cleaned and ready to cook.", 110, 70, "kg", "Green Field Farm", "Thanjavur, Tamil Nadu", 4.7),
            product("demo-groundnuts", "Organic Groundnuts", "Nuts", "Small-batch groundnuts from local growers.", 140, 50, "kg", "Anand Family Farm", "Coimbatore, Tamil Nadu", 4.8),
            product("demo-peanuts", "Raw Peanuts", "Nuts", "Fresh raw peanuts from small family farms.", 125, 40, "kg", "Anand Family Farm", "Coimbatore, Tamil Nadu", 4.6)
        );
        catalog.stream().filter(item -> !productRepository.existsById(item.getProductId())).forEach(productRepository::save);
    }

    private Product product(String id, String name, String category, String description, double price, int stock, String unit, String farmer, String location, double rating) {
        Product item = new Product(id, name, category, description, price, stock, null, rating, "Available");
        item.setUnit(unit);
        item.setFarmer(farmer);
        item.setLocation(location);
        return item;
    }
}
