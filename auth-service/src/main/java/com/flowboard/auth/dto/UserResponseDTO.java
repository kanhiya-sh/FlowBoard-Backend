package com.flowboard.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Long userId;
    private String fullName;
    private String email;
    private String username;
    private String role;
    private String avatarUrl;

    // Same boolean "is" prefix fix - serialize as "isActive" not "active"
    @JsonProperty("isActive")
    private boolean isActive;
}
