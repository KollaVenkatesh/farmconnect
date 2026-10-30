package com.klu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.klu.dto.AuthResponse;
import com.klu.dto.SigninRequest;
import com.klu.dto.SignupRequest;
import com.klu.model.Role;
import com.klu.model.User;
import com.klu.repository.UserRepository;

class UserServiceSecurityTest {

    @Test
    void signupAndSignin_shouldKeepPublicSignupAsUserAndReturnJwtPayload() {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        JwtService jwtService = new JwtService("test-secret-key-with-at-least-32-bytes", 86400000L);
        UserService service = new UserService(repository, passwordEncoder, jwtService, mock(EmailService.class));

        AtomicReference<User> savedUser = new AtomicReference<>();
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            savedUser.set(user);
            return user;
        }).when(repository).save(any(User.class));

        when(repository.existsByEmail("admin@gmail.com")).thenReturn(false);

        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setName("Admin");
        signupRequest.setEmail("admin@gmail.com");
        signupRequest.setPassword("secret123");
        signupRequest.setRole(Role.ADMIN);

        assertEquals("User registered successfully", service.signup(signupRequest));

        when(repository.findByEmail("admin@gmail.com")).thenReturn(Optional.of(savedUser.get()));

        SigninRequest signinRequest = new SigninRequest();
        signinRequest.setEmail("admin@gmail.com");
        signinRequest.setPassword("secret123");

        AuthResponse response = service.signin(signinRequest);

        assertNotNull(response);
        assertEquals(Role.USER, response.getRole());
        assertTrue(response.getToken().startsWith("eyJ"));
        assertTrue(passwordEncoder.matches("secret123", savedUser.get().getPassword()));
        assertTrue(savedUser.get().getPassword() != null && !savedUser.get().getPassword().equals("secret123"));
        assertEquals(Role.USER, savedUser.get().getRole());
    }
}
