package com.flowboard.label.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_labels", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cardId", "labelId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cardLabelId;

    @Column(nullable = false)
    private Long cardId;

    @Column(nullable = false)
    private Long labelId;

    private LocalDateTime assignedAt;

    @PrePersist
    public void onCreate() {
        this.assignedAt = LocalDateTime.now();
    }
}

