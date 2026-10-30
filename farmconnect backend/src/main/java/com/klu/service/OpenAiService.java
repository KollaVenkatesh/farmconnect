package com.klu.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klu.dto.RecipeSuggestionResponse;
import com.klu.dto.RecipeSuggestionResponse.MatchedIngredient;
import com.klu.model.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Service
public class OpenAiService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final List<String> JUICE_FRUIT_KEYWORDS = List.of(
            "mango", "banana", "papaya", "guava", "watermelon", "pomegranate", "grape",
            "orange", "apple", "pineapple", "lemon", "lime", "mosambi", "sweet lime",
            "chikoo", "sapota", "strawberry", "kiwi", "pear", "peach", "plum", "coconut");
    private static final List<String> JUICE_NONFRUIT_KEYWORDS = List.of(
            "carrot", "tomato", "chilli", "chili", "cabbage", "cauliflower", "brinjal",
            "eggplant", "okra", "ladies finger", "spinach", "onion", "potato", "beans",
            "cucumber", "pumpkin", "rice", "millet", "groundnut", "peanut");

    private final String apiKey;
    private final String model;
    private final RestClient restClient;

    @Autowired
    public OpenAiService(
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4.1-mini}") String model,
            @Value("${openai.base-url:https://api.openai.com}") String baseUrl) {
        this(apiKey, model, RestClient.builder().baseUrl(baseUrl).build());
    }

    OpenAiService(String apiKey, String model, RestClient restClient) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "gpt-4.1-mini" : model.trim();
        this.restClient = restClient;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public String chat(String message, List<String> productNames) {
        if (!isConfigured()) {
            return "OpenAI is not configured. Set the OPENAI_API_KEY environment variable to enable AI chat.";
        }

        String catalog = productNames == null || productNames.isEmpty()
                ? "No catalog products are currently available."
                : String.join(", ", productNames);
        String prompt = "You are FarmConnect's helpful assistant for farming, cooking, and local produce. "
                + "Answer clearly and concisely. Use this product catalog when relevant and do not invent "
                + "catalog items:\n" + catalog + "\n\nUser: " + message;
        return generateText(prompt, false);
    }

    public RecipeSuggestionResponse generateRecipe(String dishQuery, List<Product> products) {
        List<Product> catalog = products == null ? List.of() : products;
        if (!isConfigured()) {
            return fallbackRecipe(dishQuery, catalog);
        }

        StringBuilder catalogText = new StringBuilder();
        for (Product product : catalog) {
            if (product != null && product.getProductId() != null) {
                catalogText.append("- id: ").append(product.getProductId())
                        .append(", name: ").append(product.getProductName())
                        .append(", category: ").append(product.getCategory())
                        .append(", price: ").append(product.getPrice())
                        .append(", unit: ").append(product.getUnit()).append('\n');
            }
        }

        String prompt = "Create a complete, practical recipe for: " + dishQuery + ". "
                + "Write for a home cook and make the recipe detailed enough to follow without guessing. "
                + "Return only a JSON object with fields dish, prepTime, cookTime, servings, description, "
                + "pantryItems (array of strings with a useful quantity for each staple), instructions "
                + "(array of 8 to 12 ordered strings), and matchedIngredients (array of objects with "
                + "productId, role, and suggestedQuantity). Each instruction must explain a concrete "
                + "action and include relevant ingredient amounts, heat level, cooking time, or visual "
                + "doneness cues. Include preparation, seasoning, cooking, finishing, and serving steps; "
                + "do not collapse the recipe into a short summary. Use metric quantities where possible. "
                + "Use only matching products from this FarmConnect catalog, exact catalog product IDs, "
                + "and suggestedQuantity values from 1 to 20. Do not invent catalog products.\nCatalog:\n" + catalogText;
        String responseText;
        try {
            responseText = generateText(prompt, true);
        } catch (RuntimeException exception) {
            return catalogRecipeFallback(dishQuery, catalog);
        }

        try {
            JsonNode recipe = OBJECT_MAPPER.readTree(stripCodeFence(responseText));
            return toRecipeResponse(dishQuery, recipe, catalog);
        } catch (Exception exception) {
            return catalogRecipeFallback(dishQuery, catalog);
        }
    }

    private RecipeSuggestionResponse catalogRecipeFallback(String dishQuery, List<Product> catalog) {
        RecipeSuggestionResponse fallback = fallbackRecipe(dishQuery, catalog);
        fallback.setDescription("AI recipe generation is temporarily unavailable. This recipe uses the FarmConnect catalog guide. "
                + fallback.getDescription());
        return fallback;
    }

    private String generateText(String prompt, boolean jsonResponse) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        request.put("temperature", 0.4);
        if (jsonResponse) {
            request.put("response_format", Map.of("type", "json_object"));
        }

        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri("/v1/chat/completions")
                    .header(org.springframework.http.HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            String code = openAiErrorCode(exception.getResponseBodyAsString());
            throw new RecipeAiException("OpenAI rejected the request (HTTP "
                    + exception.getStatusCode().value() + ", " + code + ").");
        } catch (RestClientException exception) {
            throw new RecipeAiException("Could not connect to OpenAI. Check backend network access and OPENAI_BASE_URL.", exception);
        }

        JsonNode response;
        try {
            response = responseBody == null ? null : OBJECT_MAPPER.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            throw new RecipeAiException("OpenAI returned an invalid response.", exception);
        }

        JsonNode text = response == null
                ? null
                : response.path("choices").path(0).path("message").path("content");
        if (text == null || !text.isTextual() || text.asText().isBlank()) {
            throw new RecipeAiException("OpenAI returned no generated content.");
        }
        return text.asText();
    }

    private String openAiErrorCode(String body) {
        try {
            JsonNode error = OBJECT_MAPPER.readTree(body).path("error");
            String code = error.path("code").asText();
            if (code.isBlank() || "null".equals(code)) {
                code = error.path("type").asText();
            }
            return code.matches("[A-Za-z0-9_.-]{1,60}") ? code : "request_failed";
        } catch (Exception ignored) {
            return "request_failed";
        }
    }

    private RecipeSuggestionResponse toRecipeResponse(String query, JsonNode recipe, List<Product> catalog) {
        Map<String, Product> productsById = new HashMap<>();
        for (Product product : catalog) {
            if (product != null && product.getProductId() != null) {
                productsById.put(product.getProductId(), product);
            }
        }

        List<MatchedIngredient> matched = new ArrayList<>();
        JsonNode ingredients = recipe.path("matchedIngredients");
        if (!ingredients.isArray()) {
            ingredients = recipe.path("matchedProducts");
        }
        if (ingredients.isArray()) {
            for (JsonNode ingredient : ingredients) {
                Product product = productsById.get(textValue(ingredient, "productId"));
                if (product == null) {
                    continue;
                }
                int quantity = Math.max(1, Math.min(20, ingredient.path("suggestedQuantity").asInt(1)));
                matched.add(new MatchedIngredient(
                        product.getProductId(),
                        product.getProductName(),
                        product.getCategory(),
                        product.getPrice(),
                        product.getUnit(),
                        product.getFarmer(),
                        product.getLocation(),
                        product.getImageUrl(),
                        textValue(ingredient, "role"),
                        quantity));
            }
        }

        double estimatedTotal = matched.stream()
                .filter(item -> item.getPrice() != null)
                .mapToDouble(item -> item.getPrice() * item.getSuggestedQuantity())
                .sum();

        return new RecipeSuggestionResponse(
                nonBlank(textValue(recipe, "dish"), query),
                textValue(recipe, "prepTime"),
                textValue(recipe, "cookTime"),
                textValue(recipe, "servings"),
                textValue(recipe, "description"),
                matched,
                stringList(recipe.path("pantryItems")),
                stringList(recipe.path("instructions")),
                estimatedTotal);
    }

    private RecipeSuggestionResponse fallbackJuiceRecipe(String query, List<Product> catalog) {
        String normalizedQuery = query.toLowerCase(java.util.Locale.ROOT);
        List<String> requestedFruits = JUICE_FRUIT_KEYWORDS.stream()
                .filter(normalizedQuery::contains)
                .toList();
        List<MatchedIngredient> matched = new ArrayList<>();
        List<String> selectedFruitTypes = new ArrayList<>();
        for (Product product : catalog) {
            if (product == null || product.getProductId() == null || product.getProductName() == null) {
                continue;
            }
            String name = product.getProductName().toLowerCase(java.util.Locale.ROOT);
            String category = product.getCategory() == null ? "" : product.getCategory().toLowerCase(java.util.Locale.ROOT);
            boolean hasFruitName = JUICE_FRUIT_KEYWORDS.stream().anyMatch(name::contains);
            boolean hasVegetableName = JUICE_NONFRUIT_KEYWORDS.stream().anyMatch(name::contains);
            boolean isFruit = category.contains("fruit") || (hasFruitName && !hasVegetableName);
            boolean matchesRequestedFruit = requestedFruits.isEmpty()
                    ? isFruit
                    : requestedFruits.stream().anyMatch(name::contains);
            if (isFruit && matchesRequestedFruit) {
                String fruitType = JUICE_FRUIT_KEYWORDS.stream()
                        .filter(name::contains)
                        .findFirst()
                        .orElse(name);
                if (!selectedFruitTypes.contains(fruitType)) {
                    selectedFruitTypes.add(fruitType);
                    matched.add(toMatchedIngredient(product, "Fruit for juice"));
                }
                if (matched.size() == 3) {
                    break;
                }
            }
        }

        double estimatedTotal = matched.stream()
                .filter(item -> item.getPrice() != null)
                .mapToDouble(item -> item.getPrice() * item.getSuggestedQuantity())
                .sum();
        StringBuilder fruitNames = new StringBuilder();
        for (MatchedIngredient ingredient : matched) {
            if (ingredient == null) {
                continue;
            }
            String productName = ingredient.getProductName();
            if (productName == null || productName.isBlank()) {
                continue;
            }
            if (fruitNames.length() > 0) {
                fruitNames.append(", ");
            }
            fruitNames.append(productName);
        }
        String description = fruitNames.length() == 0
                ? "A quick, no-cook juice. No matching fruit is currently listed in the store, so choose your preferred fruit at home."
                : "A quick, no-cook juice using " + fruitNames + ".";

        return new RecipeSuggestionResponse(
                nonBlank(query, "Fresh Fruit Juice"),
                "10 mins",
                "0 mins",
                "2 glasses",
                description,
                matched,
                List.of("Chilled water (120�240 ml)", "Ice cubes (optional)", "Honey or sugar (optional)"),
                List.of(
                        "Wash the selected fruit under clean running water. Remove any stems, damaged spots, peels, pits, or seeds that are not edible.",
                        "Cut the fruit into small, even pieces. Measure about 2 cups of prepared fruit for two glasses; use less water for juicy fruit such as watermelon.",
                        "Put the fruit in a blender with 120 ml of chilled water. Add ice now if your blender can safely crush ice.",
                        "Blend on high for 45 to 60 seconds, stopping once to scrape down the sides if needed, until the mixture is smooth.",
                        "Taste the juice. Add more chilled water a little at a time to thin it, or add a teaspoon of honey or sugar if the fruit needs sweetness; blend for another 10 seconds.",
                        "For a smoother drink, pour through a fine sieve and press gently with a spoon. Skip straining if you prefer the fruit fiber.",
                        "Pour into two glasses and serve immediately over ice. Refrigerate any leftover juice in a covered container and drink it the same day."
                ),
                estimatedTotal);
    }

    private List<MatchedIngredient> fallbackRecipeIngredients(String dish, List<Product> catalog) {
        String query = dish.toLowerCase(java.util.Locale.ROOT);
        List<MatchedIngredient> matched = new ArrayList<>();

        if (containsAny(query, "biryani", "pulao", "fried rice")) {
            addFirstCatalogMatch(catalog, matched, "Rice base", "grain", "rice");
            addFirstCatalogMatch(catalog, matched, "Aromatic", "vegetable", "onion");
            addFirstCatalogMatch(catalog, matched, "Vegetable", "vegetable", "tomato");
            addFirstCatalogMatch(catalog, matched, "Vegetable", "vegetable", "carrot");
            addFirstCatalogMatch(catalog, matched, "Vegetable", "vegetable", "beans");
            addFirstCatalogMatch(catalog, matched, "Seasoning", "vegetable", "chilli", "chili");
        } else if (query.contains("khichdi")) {
            addFirstCatalogMatch(catalog, matched, "Grain base", "grain", query.contains("millet") ? "millet" : "rice");
            addFirstCatalogMatch(catalog, matched, "Vegetable", "vegetable", "carrot");
            addFirstCatalogMatch(catalog, matched, "Vegetable", "vegetable", "beans");
            addFirstCatalogMatch(catalog, matched, "Aromatic", "vegetable", "onion");
        } else if (query.contains("salad")) {
            addFirstCatalogMatch(catalog, matched, "Salad vegetable", "vegetable", "cucumber");
            addFirstCatalogMatch(catalog, matched, "Salad vegetable", "vegetable", "tomato");
            addFirstCatalogMatch(catalog, matched, "Salad vegetable", "vegetable", "carrot");
        } else if (query.contains("aloo gobi")) {
            addFirstCatalogMatch(catalog, matched, "Potato", "vegetable", "potato");
            addFirstCatalogMatch(catalog, matched, "Cauliflower", "vegetable", "cauliflower");
            addFirstCatalogMatch(catalog, matched, "Aromatic", "vegetable", "onion");
        } else if (containsAny(query, "sambar", "rasam", "soup")) {
            addFirstCatalogMatch(catalog, matched, "Vegetable", "vegetable", "tomato");
            addFirstCatalogMatch(catalog, matched, "Vegetable", "vegetable", "carrot");
            addFirstCatalogMatch(catalog, matched, "Vegetable", "vegetable", "beans");
            addFirstCatalogMatch(catalog, matched, "Aromatic", "vegetable", "onion");
            addFirstCatalogMatch(catalog, matched, "Seasoning", "vegetable", "chilli", "chili");
            if (query.contains("sambar")) {
                addFirstCatalogMatch(catalog, matched, "Vegetable", "vegetable", "brinjal", "eggplant");
                addFirstCatalogMatch(catalog, matched, "Vegetable", "vegetable", "pumpkin");
            }
        } else if (containsAny(query, "palak", "spinach", "dal")) {
            addFirstCatalogMatch(catalog, matched, "Leafy green", "vegetable", "spinach");
            addFirstCatalogMatch(catalog, matched, "Vegetable", "vegetable", "tomato");
            addFirstCatalogMatch(catalog, matched, "Aromatic", "vegetable", "onion");
        } else if (containsAny(query, "bhindi", "okra", "ladies finger")) {
            addFirstCatalogMatch(catalog, matched, "Main vegetable", "vegetable", "ladies finger", "okra");
            addFirstCatalogMatch(catalog, matched, "Aromatic", "vegetable", "onion");
            addFirstCatalogMatch(catalog, matched, "Seasoning", "vegetable", "chilli", "chili");
        }
        return matched;
    }

    private void addFirstCatalogMatch(
            List<Product> catalog,
            List<MatchedIngredient> matched,
            String role,
            String categoryKeyword,
            String... nameKeywords) {
        for (Product product : catalog) {
            if (product == null || product.getProductId() == null || product.getProductName() == null) {
                continue;
            }
            String category = product.getCategory() == null ? "" : product.getCategory().toLowerCase(java.util.Locale.ROOT);
            String name = product.getProductName().toLowerCase(java.util.Locale.ROOT);
            if (!category.contains(categoryKeyword) || !containsAny(name, nameKeywords)) {
                continue;
            }
            boolean alreadyAdded = false;
            for (MatchedIngredient item : matched) {
                if (item != null && product.getProductId().equals(item.getProductId())) {
                    alreadyAdded = true;
                    break;
                }
            }
            if (!alreadyAdded) {
                matched.add(toMatchedIngredient(product, role));
                return;
            }
        }
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }
    private static boolean isJuiceRequest(String query) {
        String normalized = query == null ? "" : query.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("juice") || normalized.contains("smoothie")
                || normalized.contains("milkshake") || normalized.contains("shake");
    }
    private RecipeSuggestionResponse fallbackRecipe(String query, List<Product> catalog) {
        String dish = nonBlank(query, "Farm Fresh Recipe");
        if (isJuiceRequest(dish)) {
            return fallbackJuiceRecipe(dish, catalog);
        }
        if (dish.toLowerCase(java.util.Locale.ROOT).contains("sambar")) {
            return fallbackSambarRecipe(dish, catalog);
        }

        List<MatchedIngredient> matched = fallbackRecipeIngredients(dish, catalog);
        if (matched.isEmpty()) {
            for (Product product : catalog) {
                if (product == null || product.getProductId() == null || product.getProductName() == null) {
                    continue;
                }
                if (dish.toLowerCase(java.util.Locale.ROOT).contains(product.getProductName().toLowerCase(java.util.Locale.ROOT))) {
                    matched.add(toMatchedIngredient(product, "Main ingredient"));
                    if (matched.size() == 3) {
                        break;
                    }
                }
            }
        }

        double estimatedTotal = matched.stream()
                .filter(item -> item.getPrice() != null)
                .mapToDouble(item -> item.getPrice() * item.getSuggestedQuantity())
                .sum();
        return new RecipeSuggestionResponse(
                dish,
                "15 mins",
                "25 mins",
                "2 servings",
                "A simple recipe made with fresh ingredients from the FarmConnect catalog.",
                matched,
                List.of("Salt", "Cooking oil", "Water"),
                List.of(
                        "Rinse the fresh produce under clean running water and drain it well. Set aside the pantry staples and measure them before you begin.",
                        "Prepare each ingredient: peel what needs peeling, remove tough stems, and cut the vegetables into similarly sized pieces so they cook evenly.",
                        "For two servings, heat about 1 tablespoon of oil in a pan over medium heat. Add any whole spices from your pantry and stir for 20 to 30 seconds until fragrant.",
                        "Add chopped aromatics such as onion, garlic, or ginger if available. Cook for 3 to 5 minutes, stirring often, until softened and lightly golden.",
                        "Add the firmest vegetables first with a pinch of salt and the suggested spices. Stir for 2 to 3 minutes so the pieces are evenly coated.",
                        "Add the remaining vegetables and about 120 ml water or stock. Cover and cook over medium-low heat for 8 to 15 minutes, checking and stirring occasionally until nearly tender.",
                        "Uncover and simmer for 2 to 5 minutes to reach the consistency you want. Add small splashes of water if the pan becomes dry, or simmer longer if it is too thin.",
                        "Taste and adjust salt, spice, and acidity. Finish with fresh herbs or lemon if available, then rest for 2 minutes so the flavors settle.",
                        "Serve hot with rice, bread, or another suitable staple. Check that every vegetable is tender but still holds its shape."
                ),
                estimatedTotal);
    }

    private RecipeSuggestionResponse fallbackSambarRecipe(String dish, List<Product> catalog) {
        List<MatchedIngredient> matched = fallbackRecipeIngredients(dish, catalog);
        double estimatedTotal = matched.stream()
                .filter(item -> item.getPrice() != null)
                .mapToDouble(item -> item.getPrice() * item.getSuggestedQuantity())
                .sum();

        return new RecipeSuggestionResponse(
                dish,
                "15 mins",
                "30 mins",
                "2 servings",
                "A traditional South Indian sambar with fresh FarmConnect vegetables and toor dal.",
                matched,
                List.of(
                        "Toor dal (1 cup)",
                        "Tamarind extract (2 tbsp)",
                        "Sambar powder (2 tbsp)",
                        "Mustard seeds and curry leaves",
                        "Asafoetida (hing)",
                        "Turmeric and salt",
                        "Cooking oil or ghee (1 tbsp)"),
                List.of(
                        "Rinse 1 cup of toor dal until the water runs mostly clear. Pressure-cook it with 3 cups of water and 1/4 teaspoon turmeric for 3 to 4 whistles, until soft; mash and set aside.",
                        "Wash the selected tomatoes, carrots, beans, onions, chillies, brinjal, and pumpkin. Chop them into bite-size pieces so they cook evenly.",
                        "Soak tamarind in 1/2 cup of warm water for 10 minutes. Squeeze it, strain out the pulp, and measure 2 tablespoons of extract.",
                        "Add the chopped vegetables, tamarind extract, 2 tablespoons of sambar powder, salt, and 2 cups of water to a pot. Bring to a boil, then simmer uncovered for 10 to 12 minutes until the vegetables are nearly tender.",
                        "Stir in the mashed dal. Add hot water if needed for a pourable consistency and simmer gently for 5 to 7 minutes, stirring so the dal does not stick.",
                        "Heat 1 tablespoon of oil or ghee in a small pan. Add 1/2 teaspoon mustard seeds; when they pop, add curry leaves and a pinch of asafoetida, then pour the tempering into the sambar.",
                        "Taste and adjust salt or tamarind. Simmer for 1 minute, turn off the heat, and serve hot with rice or idli."),
                estimatedTotal);
    }

    private MatchedIngredient toMatchedIngredient(Product product, String role) {
        return new MatchedIngredient(
                product.getProductId(),
                product.getProductName(),
                product.getCategory(),
                product.getPrice(),
                product.getUnit(),
                product.getFarmer(),
                product.getLocation(),
                product.getImageUrl(),
                role,
                1);
    }

    private static List<String> stringList(JsonNode values) {
        List<String> result = new ArrayList<>();
        if (values.isArray()) {
            values.forEach(value -> {
                if (value.isTextual() && !value.asText().isBlank()) {
                    result.add(value.asText());
                }
            });
        }
        return result;
    }

    private static String textValue(JsonNode value, String field) {
        JsonNode fieldValue = value.path(field);
        return fieldValue.isTextual() ? fieldValue.asText() : null;
    }

    private static String nonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String stripCodeFence(String text) {
        String value = text.trim();
        if (value.startsWith("```")) {
            int firstLineEnd = value.indexOf('\n');
            int lastFence = value.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                return value.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        return value;
    }
}
