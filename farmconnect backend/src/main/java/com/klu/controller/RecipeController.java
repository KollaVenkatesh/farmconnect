package com.klu.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.klu.service.RecipeAiException;
import com.klu.service.RecipeService;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/suggestions")
    public List<Map<String, String>> getSuggestions() {
        return recipeService.getPopularSuggestions();
    }

    @GetMapping("/match")
    public ResponseEntity<?> matchRecipeGet(@RequestParam(value = "dish", defaultValue = "Vegetable Sambar") String dish) {
        return matchRecipe(dish);
    }

    @PostMapping("/match")
    public ResponseEntity<?> matchRecipePost(@RequestBody(required = false) Map<String, Object> body) {
        String dish = "Vegetable Sambar";
        if (body != null) {
            if (body.get("dish") != null && !String.valueOf(body.get("dish")).isBlank()) {
                dish = String.valueOf(body.get("dish")).trim();
            } else if (body.get("recipeName") != null && !String.valueOf(body.get("recipeName")).isBlank()) {
                dish = String.valueOf(body.get("recipeName")).trim();
            } else if (body.get("query") != null && !String.valueOf(body.get("query")).isBlank()) {
                dish = String.valueOf(body.get("query")).trim();
            }
        }
        return matchRecipe(dish);
    }

    private ResponseEntity<?> matchRecipe(String dish) {
        try {
            return ResponseEntity.ok(recipeService.recommendRecipe(dish));
        } catch (RecipeAiException exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", exception.getMessage()));
        }
    }
}
