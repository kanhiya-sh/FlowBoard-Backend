package com.flowboard.comment.dto;

import lombok.Data;

@Data
public class UserResponseDTO {
    private Long userId;
    private String fullName;
    private String email;
    private String username;
    private String role;
    private Boolean isActive;
}
