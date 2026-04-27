package com.flowboard.workspace.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
