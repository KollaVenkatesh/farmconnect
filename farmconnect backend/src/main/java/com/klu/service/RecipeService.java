package com.klu.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.klu.dto.RecipeSuggestionResponse;
import com.klu.repository.ProductRepository;

@Service
public class RecipeService {
    private final ProductRepository productRepository;
    private final OpenAiRecipeService openAiRecipeService;

    public RecipeService(ProductRepository productRepository, OpenAiRecipeService openAiRecipeService) {
        this.productRepository = productRepository;
        this.openAiRecipeService = openAiRecipeService;
    }

    public List<Map<String, String>> getPopularSuggestions() {
        return List.of(
                Map.of("id", "sambar", "name", "South Indian Veg Sambar", "icon", "🍲", "tagline", "Farm vegetables simmered in aromatic toor dal"),
                Map.of("id", "biryani", "name", "Vegetable Dum Biryani", "icon", "🍛", "tagline", "Fragrant rice layered with fresh farm veggies"),
                Map.of("id", "rasam", "name", "Farm Tomato Rasam", "icon", "🍅", "tagline", "Tangy, soothing tomato broth with pepper and chillies"),
                Map.of("id", "aloo-gobi", "name", "Aloo Gobi Masala", "icon", "🥔", "tagline", "Tender potatoes and fresh cauliflower florets"),
                Map.of("id", "salad", "name", "Fresh Garden Salad", "icon", "🥗", "tagline", "Crunchy cucumbers, ripe tomatoes, carrots, and onions"),
                Map.of("id", "dal-palak", "name", "Homestyle Spinach Dal", "icon", "🥬", "tagline", "Leafy green spinach tempered with garlic and cumin"),
                Map.of("id", "bhindi-fry", "name", "Crispy Bhindi Fry", "icon", "🌿", "tagline", "Tender ladies finger roasted with onions and spices"),
                Map.of("id", "fruit-bowl", "name", "Seasonal Fruit Platter", "icon", "🍉", "tagline", "Alphonso mangoes, sweet papaya, watermelon & pomegranate"),
                Map.of("id", "millet-khichdi", "name", "Healthy Millet Khichdi", "icon", "🌾", "tagline", "Wholesome foxtail millet cooked with crunchy vegetables"));
    }

    public RecipeSuggestionResponse recommendRecipe(String dishQuery) {
        String query = dishQuery == null || dishQuery.isBlank() ? "Vegetable Sambar" : dishQuery.trim();
        List<com.klu.model.Product> catalog;
        try {
            catalog = productRepository.findAll();
        } catch (RuntimeException exception) {
            // Keep recipe guidance available during a temporary catalog database outage.
            catalog = List.of();
        }
        return openAiRecipeService.generateRecipe(query, catalog);
    }
}
