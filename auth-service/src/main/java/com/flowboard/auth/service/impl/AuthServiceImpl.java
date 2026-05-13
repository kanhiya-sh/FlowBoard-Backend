package com.flowboard.auth.service.impl;

import com.flowboard.auth.dto.*;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.exception.ResourceNotFoundException;
import com.flowboard.auth.repository.UserRepository;
import com.flowboard.auth.service.AuthService;
import com.flowboard.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("MEMBER")
                .provider("LOCAL")
                .isActive(true)
                .build();

        userRepository.save(user);
        return "User registered successfully";
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + request.getEmail()));

        if (!user.isActive()) {
            throw new IllegalStateException("Account is deactivated. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid password");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponse(
                token,
                user.getUserId(),
                user.getEmail(),
                user.getFullName(),
                user.getUsername(),
                user.getRole(),
                user.getAvatarUrl(),
                user.isActive(),
                "Login successful"
        );
    }

    @Override
    @Cacheable(value = "users_by_email", key = "#email")
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));
        return mapToDTO(user);
    }

    @Override
    @Cacheable(value = "users_by_id", key = "#userId")
    public UserResponseDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + userId));
        return mapToDTO(user);
    }

    // ─── Profile Management ────────────────────────────────────────────────────

    @Override
    @Caching(evict = {
            @CacheEvict(value = "users_by_id", key = "#userId"),
            @CacheEvict(value = "users_by_email", allEntries = true),
            @CacheEvict(value = "users_by_username", allEntries = true)
    })
    public UserResponseDTO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            // Check uniqueness only if changing to a different username
            if (!request.getUsername().equals(user.getUsername())
                    && userRepository.existsByUsername(request.getUsername())) {
                throw new IllegalArgumentException("Username already taken");
            }
            user.setUsername(request.getUsername());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        return mapToDTO(userRepository.save(user));
    }

    @Override
    @CacheEvict(value = "users_by_id", key = "#userId")
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "users_by_id", key = "#userId"),
            @CacheEvict(value = "users_by_email", allEntries = true),
            @CacheEvict(value = "users_by_username", allEntries = true)
    })
    public void deactivateAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        user.setActive(false);
        userRepository.save(user);
    }

    // ─── Mapper ────────────────────────────────────────────────────────────────

    private UserResponseDTO mapToDTO(User user) {
        return UserResponseDTO.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .username(user.getUsername())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.isActive())
                .build();
    }

    @Override
    @Cacheable(value = "users_by_username", key = "#username")
    public UserResponseDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToDTO(user);
    }

    @Override
    public List<UserResponseDTO> searchUsers(String query) {
        return userRepository.searchByFullName(query)
                .stream().map(this::mapToDTO).toList();
    }
}
