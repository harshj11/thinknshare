package com.app.thinknshare.user.service;

import com.app.thinknshare.user.entity.RefreshToken;
import com.app.thinknshare.user.exception.RefreshException;
import com.app.thinknshare.user.repository.RefreshTokenRepository;
import com.app.thinknshare.user.util.RefreshProps;
import com.app.thinknshare.user.util.TokenUtil;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional
@AllArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repo;


    private RefreshProps refreshProps;

    /** Issues a new refresh token for userId, returns plaintext token to set as cookie */
    public String issue(UUID userId, @Nullable String deviceId, String userAgent, String ipAddress) {
        String plain = TokenUtil.generateOpaqueToken();
        String hash = TokenUtil.covertToSha256Hash(plain);

        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setTokenHash(hash);
        rt.setIssuedAt(Instant.now());
        rt.setExpiresAt(Instant.now().plus(refreshProps.getExpiresDays(), ChronoUnit.DAYS));
        rt.setDeviceId(deviceId);
        rt.setUserAgent(userAgent);
        rt.setIpAddress(ipAddress);
        repo.save(rt);

        return plain; // store hash, return plaintext to client
    }

    /** Verifies and rotates the token. Returns the *new* plaintext refresh token. */
    public String verifyAndRotate(String incomingPlainToken) {
        String incomingHash = TokenUtil.covertToSha256Hash(incomingPlainToken);
        RefreshToken current = repo.findByTokenHash(incomingHash)
            .orElseThrow(() -> new RefreshException("invalid_refresh", "Refresh token not found!"));

        // Reuse attack detection: if someone presents a token that was already rotated/revoked → nuke sessions
        if (current.getRevokedAt() != null) {
            // If replacedByTokenHash is set, this token was rotated earlier
            if (current.getReplacedByTokenHash() != null) {
                repo.revokeAllActiveByUserId(current.getUserId(), Instant.now()); // global logout
                throw new RefreshException("reuse_detected", "Refresh token reuse detected; all sessions revoked");
            }
            throw new RefreshException("revoked_refresh", "Refresh token has been revoked!");
        }

        if (Instant.now().isAfter(current.getExpiresAt())) {
            throw new RefreshException("expired_refresh", "Refresh token expired!");
        }

        // Rotate: revoke current and issue a new one
        String newPlain = TokenUtil.generateOpaqueToken();
        String newHash = TokenUtil.covertToSha256Hash(newPlain);

        current.setRevokedAt(Instant.now());
        current.setReplacedByTokenHash(newHash);
        repo.save(current);

        RefreshToken next = new RefreshToken();
        next.setUserId(current.getUserId());
        next.setTokenHash(newHash);
        next.setIssuedAt(Instant.now());
        next.setExpiresAt(current.getIssuedAt().plus(refreshProps.getExpiresDays(), ChronoUnit.DAYS));
        next.setDeviceId(current.getDeviceId());
        next.setUserAgent(current.getUserAgent());
        next.setIpAddress(current.getIpAddress());
        repo.save(next);

        return newPlain;
    }

    /** Revoke a single refresh token */
    public void revoke(String incomingPlainToken) {
        String hash = TokenUtil.covertToSha256Hash(incomingPlainToken);
        RefreshToken rt = repo.findByTokenHash(hash)
            .orElseThrow(() -> new RefreshException("invalid_refresh", "Refresh token not found!"));
        if (rt.getRevokedAt() == null) {
            rt.setRevokedAt(Instant.now());
            repo.save(rt);
        }
    }

    /** Revoke all tokens for user (logout everywhere) */
    public int revokeAllForUser(UUID userId) {
        return repo.revokeAllActiveByUserId(userId, Instant.now());
    }

    public UUID getUserIdFromRefreshToken(String incomingPlainToken) {
        String incomingHash = TokenUtil.covertToSha256Hash(incomingPlainToken);
        RefreshToken current = repo.findByTokenHash(incomingHash)
            .orElseThrow(() -> new RefreshException("invalid_refresh", "Refresh token not found!"));
        return current.getUserId();
    }
}
