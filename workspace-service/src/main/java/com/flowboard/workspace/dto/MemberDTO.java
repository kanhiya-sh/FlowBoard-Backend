package com.flowboard.workspace.dto;

import com.flowboard.workspace.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDTO {
    private Long userId;
    private String fullName;
    private String email;
    private String username;
    private String avatarUrl;
    private Role role;
}