package com.familyvault.service;

import com.familyvault.model.User;
import com.familyvault.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final JwtService jwtService;

    @Value("${app.admin-email}")
    private String adminEmail;

    @Value("${app.allowed-emails}")
    private String allowedEmailsRaw;

    public AuthService(UserRepository userRepo, JwtService jwtService) {
        this.userRepo = userRepo;
        this.jwtService = jwtService;
    }

    public Set<String> getAllowedEmails() {
        Set<String> allowed = Arrays.stream(allowedEmailsRaw.split(","))
                .map(e -> e.trim().toLowerCase())
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toSet());
        if (adminEmail != null && !adminEmail.isBlank()) {
            allowed.add(adminEmail.trim().toLowerCase());
        }
        return allowed;
    }

    public boolean isEmailAllowed(String email) {
        return getAllowedEmails().contains(email.trim().toLowerCase());
    }

    public boolean isAdmin(String email) {
        return email != null && adminEmail != null
                && email.trim().equalsIgnoreCase(adminEmail.trim());
    }

    public Optional<User> findUserByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public User createUser(String email, String username) {
        User user = new User(email, username);
        return userRepo.save(user);
    }

    public boolean userExists(String email) {
        return userRepo.existsByEmail(email);
    }

    public String createToken(String email) {
        return jwtService.createAccessToken(email);
    }

    public String decodeToken(String token) {
        return jwtService.decodeToken(token);
    }

    public User getCurrentUser(String token) {
        if (token == null || token.isBlank()) return null;
        String email = jwtService.decodeToken(token);
        if (email == null) return null;
        return userRepo.findByEmail(email).orElse(null);
    }
}
