package com.flowboard.list.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserResponseDTO {
    private Long userId;
    private String fullName;
    private String email;
    private String username;
    private String role;
    private String avatarUrl;

    @JsonProperty("isActive")
    private boolean isActive;
}
