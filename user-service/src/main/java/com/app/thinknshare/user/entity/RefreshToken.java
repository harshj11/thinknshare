package com.app.thinknshare.user.entity;

import com.github.f4b6a3.uuid.UuidCreator;
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
    private UUID refreshTokenId = UuidCreator.getTimeOrdered();

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash; // SHA-256 hex of the opaque refresh token

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefreshTokenStatus refreshTokenStatus = RefreshTokenStatus.ACTIVE;

    @Column(nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt; // null if active

    // For reuse detection: when rotated, the old token points to the new one's hash
    private String replacedByTokenHash;

    @Column(nullable = false)
    private UUID familyRootTokenId; // For tracking the original token in a chain

    @Column(nullable = false)
    private Integer issuedTokenVersion; // User's token version at issuance time

    // Optional session metadata (useful for "logout from device", risk auditing)
    private String deviceId;
    private String userAgent;
    private String ipAddress;

    // Concurrency safety on rotate
    @Version
    private Long version;
}
