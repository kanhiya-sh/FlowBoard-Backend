package com.flowboard.board.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight projection of a workspace member received from workspace-service
 * over Feign. Mirrors the fields workspace-service's MemberDTO ships so board-
 * service can merge workspace members into its "assignable users" list without
 * depending on workspace-service's enum type directly. Role is kept as a plain
 * String so either "ADMIN"/"MEMBER" deserializes cleanly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberDTO {
    private Long userId;
    private String fullName;
    private String email;
    private String username;
    private String avatarUrl;
    private String role;
}
