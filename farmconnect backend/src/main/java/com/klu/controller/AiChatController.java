package com.klu.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.klu.repository.ProductRepository;
import com.klu.service.OpenAiService;

/**
 * AI Chat endpoint powered by OpenAI.
 * GET  /api/ai/status  — Check if OpenAI is configured
 * POST /api/ai/chat    — Ask a farming/cooking/produce question
 */
@RestController
@RequestMapping("/api/ai")
public class AiChatController {

    private final OpenAiService openAiService;
    private final ProductRepository productRepository;

    public AiChatController(OpenAiService openAiService, ProductRepository productRepository) {
        this.openAiService = openAiService;
        this.productRepository = productRepository;
    }

    /** Returns whether the OpenAI LLM is configured and ready. */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "configured", openAiService.isConfigured(),
                "provider", "OpenAI",
                "message", openAiService.isConfigured()
                        ? "✅ OpenAI AI is active and ready."
                        : "⚠️ OpenAI API key not set. Add OPENAI_API_KEY to application-secrets.properties."
        ));
    }

    /**
     * General AI Chat endpoint.
     * Body: { "message": "What vegetables are good for sambar?" }
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        String message = body.get("message") instanceof String m ? m.trim() : "";
        if (message.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please provide a message."));
        }
        if (message.length() > 1000) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Message is too long. Please keep it under 1000 characters."));
        }

        // Build product context from live catalog
        List<String> productNames = productRepository.findAll().stream()
                .filter(p -> p.getProductName() != null)
                .map(p -> p.getProductName() + " (" + p.getCategory() + ")")
                .limit(40)
                .toList();

        String reply = openAiService.chat(message, productNames);

        return ResponseEntity.ok(Map.of(
                "reply", reply,
                "powered_by", "OpenAI"
        ));
    }
}

