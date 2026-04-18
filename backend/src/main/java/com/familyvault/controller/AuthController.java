package com.familyvault.controller;

import com.familyvault.model.User;
import com.familyvault.service.AuthService;
import com.familyvault.service.OtpService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;

    private static final int COOKIE_MAX_AGE = 60 * 60 * 72; // 72 hours

    public AuthController(AuthService authService, OtpService otpService) {
        this.authService = authService;
        this.otpService = otpService;
    }

    // ---- DTOs ----
    record EmailRequest(String email) {}
    record OtpVerifyRequest(String email, String otp) {}
    record SetUsernameRequest(String email, String username) {}

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody EmailRequest body) {
        String email = body.email().toLowerCase().trim();
        if (!authService.isEmailAllowed(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("detail", "Your not Allowed"));
        }
        if (!otpService.checkRateLimit(email)) {
            return ResponseEntity.status(429)
                    .body(Map.of("detail", "Too many otp requests. Try again Later"));
        }
        String otp = otpService.generateOtp();
        otpService.storeOtp(email, otp);
        if (!otpService.sendOtpEmail(email, otp)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("detail", "Failed to send OTP"));
        }
        return ResponseEntity.ok(Map.of("message", "OTP sent, check ur mail"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerifyRequest body, HttpServletResponse response) {
        String email = body.email().toLowerCase().trim();
        if (!authService.isEmailAllowed(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("detail", "Your not Allowed"));
        }
        if (!otpService.verifyOtp(email, body.otp())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("detail", "Invalid/expired otp"));
        }
        boolean isNew = !authService.userExists(email);
        if (!isNew) {
            String token = authService.createToken(email);
            setCookie(response, token);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "OTP verified");
        result.put("needs_username", isNew);
        result.put("email", email);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/set-username")
    public ResponseEntity<?> setUsername(@RequestBody SetUsernameRequest body, HttpServletResponse response) {
        String email = body.email().toLowerCase().trim();
        if (!authService.isEmailAllowed(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("detail", "Your not Allowed"));
        }
        String username = body.username().trim();
        if (username.length() < 2 || username.length() > 40) {
            return ResponseEntity.badRequest()
                    .body(Map.of("detail", "Username must be 2-40 character"));
        }
        if (authService.userExists(email)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("detail", "User already registered"));
        }
        authService.createUser(email, username);
        String token = authService.createToken(email);
        setCookie(response, token);
        return ResponseEntity.ok(Map.of("message", "WELCOME TO FAMILY", "username", username));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("access_token", "");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("message", "Logged Out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe(@CookieValue(name = "access_token", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("detail", "NOT REGISTERED"));
        }
        User user = authService.getCurrentUser(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("detail", "USER NOT FOUND"));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("email", user.getEmail());
        result.put("username", user.getUsername() != null ? user.getUsername() : "");
        result.put("is_admin", authService.isAdmin(user.getEmail()));
        return ResponseEntity.ok(result);
    }

    private void setCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setPath("/");
        cookie.setSecure(false); // match Python: secure=False
        response.addCookie(cookie);
    }
}
