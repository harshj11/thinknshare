package com.app.thinknshare.user.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens",
       indexes = { @Index(name = "idx_token_hash", columnList = "tokenHash"),
                   @Index(name = "idx_user_id", columnList = "userId") })
@Data
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash; // SHA-256 hex of the opaque refresh token

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt; // null if active

    // For reuse detection: when rotated, the old token points to the new one's hash
    private String replacedByTokenHash;

    // Optional session metadata (useful for "logout from device", risk auditing)
    private String deviceId;
    private String userAgent;
    private String ipAddress;

    // Concurrency safety on rotate
    @Version
    private Long version;
}
