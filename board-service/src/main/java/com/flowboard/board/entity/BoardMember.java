package com.flowboard.board.entity;

import com.flowboard.board.enums.BoardRole;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "board_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class BoardMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardMemberId;

    private Long boardId;

    private Long userId;

    @Enumerated(EnumType.STRING)
    private BoardRole role;

    private LocalDateTime addedAt;

    @PrePersist
    public void onCreate() {
        this.addedAt = LocalDateTime.now();
    }
}