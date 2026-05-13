package com.flowboard.comment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    @Column(nullable = false)
    private Long cardId;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // null = top-level comment; non-null = reply to parent
    private Long parentCommentId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isDeleted = false;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isDeleted == null) this.isDeleted = false;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
