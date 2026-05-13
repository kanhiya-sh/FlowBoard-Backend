package com.flowboard.card.dto;

import lombok.Data;

@Data
public class UserResponseDTO {
    private Long userId;
    private String fullName;
    private String email;
    private String username;
    private String role;
    private String avatarUrl;
    private Boolean isActive;
}
