package com.flowboard.board.entity;

import com.flowboard.board.enums.Visibility;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "boards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Board {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long boardId;

    private Long workspaceId;

    private String name;

    private String description;

    private String background;

    @Enumerated(EnumType.STRING)
    private Visibility visibility;

    private Long createdById;

    @Builder.Default
    private Boolean isClosed = false;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isClosed == null) this.isClosed = false;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}