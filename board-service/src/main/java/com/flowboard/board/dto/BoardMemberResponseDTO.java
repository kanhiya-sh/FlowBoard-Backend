package com.flowboard.board.dto;

import com.flowboard.board.enums.BoardRole;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardMemberResponseDTO {
    private Long boardMemberId;
    private Long boardId;
    private Long userId;
    private BoardRole role;
    private LocalDateTime addedAt;

    // Enriched lazily from Auth service so the frontend can render the user's
    // email/name without separate lookups. Stay null if Auth call fails so the
    // response payload still ships.
    private String fullName;
    private String email;
    private String username;
    private String avatarUrl;
}