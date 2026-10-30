package com.klu.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.klu.dto.RecipeSuggestionResponse;
import com.klu.model.Product;

/**
 * Recipe API facade backed by OpenAI.

 */
@Service
public class OpenAiRecipeService {

    private final OpenAiService openAiService;

    public OpenAiRecipeService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public RecipeSuggestionResponse generateRecipe(String dishQuery, List<Product> products) {
        return openAiService.generateRecipe(dishQuery, products);
    }
}
