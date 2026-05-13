package com.flowboard.list.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for checking if a user is a member of a board and their role.
// This is used to determine permissions for actions on the board.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardMemberCheckDTO {
    @JsonProperty("isMember")
    private boolean isMember;
    private String role;
}
