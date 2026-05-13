package com.flowboard.auth.controller;

import com.flowboard.auth.dto.*;
import com.flowboard.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    // Public Auth
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Profile Management (JWT protected — user provides token)
    @PutMapping("/profile/{userId}")
    public ResponseEntity<UserResponseDTO> updateProfile(
            @PathVariable Long userId,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @PutMapping("/password/{userId}")
    public ResponseEntity<String> changePassword(
            @PathVariable Long userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request);
        return ResponseEntity.ok("Password changed successfully");
    }

    @DeleteMapping("/deactivate/{userId}")
    public ResponseEntity<String> deactivateAccount(@PathVariable Long userId) {
        authService.deactivateAccount(userId);
        return ResponseEntity.ok("Account deactivated successfully");
    }

    // Internal Endpoints (service-to-service, no JWT)
    @GetMapping("/internal/users/email/{email}")
    public ResponseEntity<UserResponseDTO> getUserByEmail(@PathVariable String email) {
        return ResponseEntity.ok(authService.getUserByEmail(email));
    }

    @GetMapping("/internal/users/{userId}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @GetMapping("/internal/users/username/{username}")
    public ResponseEntity<UserResponseDTO> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(authService.getUserByUsername(username));
    }

    // Search Users use case
    @GetMapping("/search")
    public ResponseEntity<List<UserResponseDTO>> searchUsers(@RequestParam String q) {
        return ResponseEntity.ok(authService.searchUsers(q));
    }
}
