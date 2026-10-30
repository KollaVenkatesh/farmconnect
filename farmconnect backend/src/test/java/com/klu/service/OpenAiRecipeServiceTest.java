package com.klu.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.util.List;

import com.klu.model.Product;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Tests for the Gemini-powered recipe and chat service.
 * Previously tested OpenAiRecipeService; now delegates to GeminiService.
 */
class OpenAiRecipeServiceTest {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String TEST_KEY = "test-gemini-key";
    private static final String MODEL    = "gemini-1.5-flash";

    /** Wraps a recipe JSON string inside a Gemini API response envelope. */
    private static String geminiEnvelope(String recipeJson) {
        // Escape backslashes and double-quotes for embedding in JSON string value
        String escaped = recipeJson
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"" + escaped + "\"}]},\"finishReason\":\"STOP\"}]}";
    }

    @Test
    void generateRecipe_usesGeminiResponseAndOnlyMatchesProductsInTheCatalog() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        String recipeJson = "{\"dish\":\"AI Tomato Curry\",\"prepTime\":\"10 mins\",\"cookTime\":\"20 mins\","
                + "\"servings\":\"2 servings\",\"description\":\"A fresh curry.\","
                + "\"pantryItems\":[\"Salt\"],\"instructions\":[\"Cook the ingredients until tender.\"],"
                + "\"matchedIngredients\":["
                + "{\"productId\":\"tomato-1\",\"role\":\"Base\",\"suggestedQuantity\":99},"
                + "{\"productId\":\"fake-id\",\"role\":\"Invented\",\"suggestedQuantity\":1}"
                + "]}";

        server.expect(requestTo(org.hamcrest.Matchers.containsString("/models/" + MODEL + ":generateContent")))
              .andExpect(method(HttpMethod.POST))
              .andRespond(withSuccess(geminiEnvelope(recipeJson), MediaType.APPLICATION_JSON));

        // Use the package-private test constructor to inject mock RestClient
        GeminiService service = new GeminiService(TEST_KEY, MODEL, builder.build());

        Product tomato = new Product();
        tomato.setProductId("tomato-1");
        tomato.setProductName("Tomato");
        tomato.setCategory("Vegetables");
        tomato.setPrice(40.0);
        tomato.setUnit("kg");
        tomato.setStock(10);

        var recipe = service.generateRecipe("tomato curry", List.of(tomato));

        assertEquals("AI Tomato Curry", recipe.getDish());
        assertEquals(1, recipe.getMatchedProducts().size(), "Only catalog products should be matched");
        assertEquals("tomato-1", recipe.getMatchedProducts().get(0).getProductId());
        // suggestedQuantity capped at 20
        assertEquals(20, recipe.getMatchedProducts().get(0).getSuggestedQuantity());
        assertEquals(800.0, recipe.getEstimatedTotal());
        server.verify();
    }

    @Test
    void generateRecipe_withNoApiKey_returnsFallbackRecipeWithoutThrowing() {
        // GeminiService with blank key must NOT throw — it falls back gracefully
        GeminiService service = new GeminiService("", MODEL, RestClient.builder().build());

        Product potato = new Product();
        potato.setProductId("potato-1");
        potato.setProductName("Potato");
        potato.setCategory("Vegetables");
        potato.setPrice(30.0);
        potato.setUnit("kg");
        potato.setStock(5);

        var recipe = service.generateRecipe("aloo gobi", List.of(potato));

        assertNotNull(recipe, "Fallback recipe should be returned when no API key");
        assertFalse(recipe.getDish().isBlank(), "Dish name should not be blank");
        assertNotNull(recipe.getInstructions(), "Instructions should not be null");
        assertFalse(recipe.getInstructions().isEmpty(), "Instructions should have at least one step");
    }

    @Test
    void chat_withNoApiKey_returnsSetupInstructions() {
        GeminiService service = new GeminiService("", MODEL, RestClient.builder().build());
        String reply = service.chat("What vegetables are good for sambar?", List.of("Tomato", "Potato"));
        assertNotNull(reply);
        // Should tell the user to configure the API key
        assertTrue(
            reply.contains("GEMINI_API_KEY") || reply.contains("not configured") || reply.contains("API key"),
            "Reply should guide user to configure API key, but was: " + reply
        );
    }
}
