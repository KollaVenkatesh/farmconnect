package com.klu.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.klu.model.User;
import com.klu.repository.UserRepository;

class UserPreferencesControllerTest {

    @Test
    void getPreferences_returnsTheAuthenticatedUsersLanguage() {
        UserRepository users = mock(UserRepository.class);
        UserPreferencesController controller = new UserPreferencesController(users);
        User user = new User();
        user.setEmail("buyer@example.com");
        user.setPreferredLanguage("hi");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("buyer@example.com");
        when(users.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));

        ResponseEntity<Map<String, String>> response = controller.getPreferences(authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("hi", response.getBody().get("language"));
    }

    @Test
    void updatePreferences_savesSupportedLanguageToTheAuthenticatedUser() {
        UserRepository users = mock(UserRepository.class);
        UserPreferencesController controller = new UserPreferencesController(users);
        User user = new User();
        user.setEmail("buyer@example.com");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("buyer@example.com");
        when(users.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));
        when(users.save(user)).thenReturn(user);

        ResponseEntity<?> response = controller.updatePreferences(Map.of("language", "ta"), authentication);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("ta", user.getPreferredLanguage());
        verify(users).save(user);
    }

    @Test
    void updatePreferences_rejectsUnsupportedLanguageWithoutSaving() {
        UserRepository users = mock(UserRepository.class);
        UserPreferencesController controller = new UserPreferencesController(users);
        Authentication authentication = mock(Authentication.class);

        ResponseEntity<?> response = controller.updatePreferences(Map.of("language", "xx"), authentication);

        assertEquals(400, response.getStatusCode().value());
        verify(users, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }
}
