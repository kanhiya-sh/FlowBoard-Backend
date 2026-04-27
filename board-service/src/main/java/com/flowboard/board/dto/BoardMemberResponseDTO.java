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
}