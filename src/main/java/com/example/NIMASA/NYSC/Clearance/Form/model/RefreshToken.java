package com.example.NIMASA.NYSC.Clearance.Form.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 500)
    private String token; // The actual refresh token (hashed)

    private String rawToken;

    @Column(nullable = false)
    private String employeeName; // Which employee this belongs to

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean revoked = false; // For token rotation security

    @Column(nullable = true)
    private String deviceInfo; // Optional: track which device/browser

    // Token family for enhanced security
    @Column(nullable = false)
    private String tokenFamily; // UUID to group related tokens

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expirationDate);
    }

    public boolean isRevoked() {
        return this.revoked;
    }
}