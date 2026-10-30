package com.klu.controller;

import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.klu.model.User;
import com.klu.repository.UserRepository;

@RestController
@RequestMapping("/api/users/me/preferences")
public class UserPreferencesController {
    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
            "en", "ar", "as", "bn", "brx", "doi", "de", "es", "fr", "gu",
            "hi", "kn", "ks", "kok", "mai", "ml", "mni", "mr", "ja", "ne",
            "or", "pa", "pt", "ru", "sa", "sat", "sd", "ta", "te", "ur", "zh");

    private final UserRepository users;

    public UserPreferencesController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> getPreferences(Authentication authentication) {
        return users.findByEmail(authentication.getName())
                .map(user -> ResponseEntity.ok(Map.of("language", user.getPreferredLanguage())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<?> updatePreferences(
            @RequestBody Map<String, String> preferences,
            Authentication authentication) {
        String language = preferences == null ? null : preferences.get("language");
        if (language == null || !SUPPORTED_LANGUAGES.contains(language)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Choose a supported language"));
        }

        User user = users.findByEmail(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User account was not found"));
        }

        user.setPreferredLanguage(language);
        users.save(user);
        return ResponseEntity.ok(Map.of("language", user.getPreferredLanguage()));
    }
}
