package com.flowboard.board.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberCheckDTO {

    // CRITICAL: Jackson strips "is" prefix on boolean fields.
    // @JsonProperty forces correct JSON key binding when deserializing.
    @JsonProperty("isMember")
    private boolean isMember;

    private String role;
}
