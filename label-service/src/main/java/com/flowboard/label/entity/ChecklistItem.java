package com.flowboard.label.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "checklist_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @Column(nullable = false)
    private Long checklistId;

    @Column(nullable = false)
    private String text;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isCompleted = false;

    private Long assigneeId;

    private LocalDate dueDate;

    @PrePersist
    public void onCreate() {
        if (this.isCompleted == null) this.isCompleted = false;
    }
}

