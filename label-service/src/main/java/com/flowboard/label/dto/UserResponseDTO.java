package com.flowboard.label.dto;

import lombok.Data;

@Data
public class UserResponseDTO {
    private Long userId;
    private String fullName;
    private String email;
    private String username;
    private String role;
    private String avatarUrl;
    private boolean isActive;
}

