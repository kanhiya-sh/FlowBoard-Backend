package com.flowboard.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    // nullable = true — OAuth users ke paas password nahi hota
    @Column(name = "password_hash", nullable = true)
    private String passwordHash;

    @Column(unique = true, nullable = false)
    private String username;

    private String role;

    private String avatarUrl;

    // "LOCAL" = email/password login, "GOOGLE" = OAuth login
    private String provider;

    @Column(name = "is_active")
    private boolean isActive;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        // Default provider
        if (this.provider == null) {
            this.provider = "LOCAL";
        }
    }
}