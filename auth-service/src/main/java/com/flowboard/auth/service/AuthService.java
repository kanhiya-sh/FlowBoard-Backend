package com.flowboard.auth.service;

import com.flowboard.auth.dto.*;
import java.util.List;

public interface AuthService {
    String register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
    UserResponseDTO getUserByEmail(String email);
    UserResponseDTO getUserById(Long userId);

    // Profile management (spec section 2.1 — all users can update profile, username, avatar, password)
    UserResponseDTO updateProfile(Long userId, UpdateProfileRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
    void deactivateAccount(Long userId);
    UserResponseDTO getUserByUsername(String username);

    // PDF spec 3.2 — Search Users use case
    List<UserResponseDTO> searchUsers(String query);
}
