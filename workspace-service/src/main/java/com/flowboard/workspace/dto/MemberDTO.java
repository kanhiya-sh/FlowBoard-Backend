package com.flowboard.workspace.dto;

import com.flowboard.workspace.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MemberDTO {
    private Long userId;
    private Role role;
}