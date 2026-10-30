package com.klu.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.klu.dto.AuthResponse;
import com.klu.dto.SigninRequest;
import com.klu.dto.SignupRequest;
import com.klu.model.Role;
import com.klu.model.User;
import com.klu.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    public String signup(SignupRequest request) {
        String email = request.getEmail() == null ? request.getUserId() : request.getEmail();
        String username = request.getUserId() == null ? email : request.getUserId();
        if (email == null || request.getPassword() == null) throw new IllegalArgumentException("Email and password are required");
        if (userRepository.existsByEmail(email) || userRepository.existsByUsername(username)) {
            return "Email already registered";
        }

        User user = new User();
        user.setName(request.getFullName() == null ? request.getName() : request.getFullName());
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // Public signups cannot grant administrator privileges.
        user.setRole(request.getRole() == null || request.getRole() == Role.ADMIN ? Role.USER : request.getRole());

        userRepository.save(user);

        // Send Welcome Notification Email
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getName(), String.valueOf(user.getRole()));
        } catch (Exception ignored) {
            // Non-blocking notification
        }

        return "User registered successfully";
    }

    public AuthResponse signin(SigninRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim();
        String userId = request.getUserId() == null ? null : request.getUserId().trim();

        User user = null;
        if (email != null && !email.isBlank()) {
            user = userRepository.findByEmail(email)
                    .or(() -> userRepository.findByUsername(email))
                    .orElse(null);
        }

        if (user == null && userId != null && !userId.isBlank()) {
            user = userRepository.findByEmail(userId)
                    .or(() -> userRepository.findByUsername(userId))
                    .orElse(null);
        }

        if (user == null) {
            return null;
        }

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!passwordMatches) {
            return null;
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                "Sign In Successful",
                token
        );
    }
}
