package com.flowboard.card.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardMemberCheckDTO {

    // CRITICAL: board-service serializes this as "isMember" via @JsonProperty("isMember").
    // Without this annotation, Jackson maps "isMember" JSON key to nothing → always false
    // → every ensureBoardMember() call throws 403 Forbidden for all valid members.
    @JsonProperty("isMember")
    private boolean isMember;

    private String role; // OBSERVER | MEMBER | ADMIN
}
