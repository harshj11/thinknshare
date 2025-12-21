package com.app.thinknshare.user.service;

import com.app.thinknshare.user.dtos.DeviceDetails;
import com.app.thinknshare.user.dtos.RefreshTokenDetails;
import com.app.thinknshare.user.dtos.UserPrincipal;
import com.app.thinknshare.user.entity.RefreshToken;
import com.app.thinknshare.user.entity.RefreshTokenStatus;
import com.app.thinknshare.user.entity.User;
import com.app.thinknshare.user.exception.RefreshException;
import com.app.thinknshare.user.repository.RefreshTokenRepository;
import com.app.thinknshare.user.util.RefreshProps;
import com.app.thinknshare.user.util.TokenUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@AllArgsConstructor
public class RefreshTokenServiceImpl implements IRefreshTokenService {

    private final RefreshProps refreshProps;

    private final RefreshTokenRepository refreshTokenRepository;

    private final UserService userService;

    @Override
    public RefreshTokenDetails issue(UserPrincipal userPrincipal, DeviceDetails deviceDetails) {
        String base64EncodedToken = TokenUtil.generateOpaqueToken();
        String hashedToken = TokenUtil.covertToSha256Hash(base64EncodedToken);

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUserId(userPrincipal.userId());
        refreshToken.setTokenHash(hashedToken);
        refreshToken.setFamilyRootTokenId(refreshToken.getRefreshTokenId());
        refreshToken.setIssuedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().plus(refreshProps.getExpiresDays(), ChronoUnit.DAYS));
        refreshToken.setRefreshTokenStatus(RefreshTokenStatus.ACTIVE);
        refreshToken.setDeviceId(deviceDetails.deviceId());
        refreshToken.setUserAgent(deviceDetails.userAgent());
        refreshToken.setIpAddress(deviceDetails.ipAddress());
        refreshToken.setIssuedTokenVersion(userPrincipal.tokenVersion());

        refreshTokenRepository.save(refreshToken);

        return new RefreshTokenDetails(refreshToken, base64EncodedToken);
    }

    @Override
    public RefreshTokenDetails verifyAndRotate(String incomingRefreshToken, DeviceDetails deviceDetails) {
        String hashedRefreshToken = TokenUtil.covertToSha256Hash(incomingRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hashedRefreshToken)
                .orElseThrow(() -> new RefreshException("invalid_refresh", "Refresh token not found!"));

        if(token.getRefreshTokenStatus() == RefreshTokenStatus.REVOKED) {
            if(token.getReplacedByTokenHash() != null) {
                refreshTokenRepository.revokeAllActiveByUserId(token.getUserId(), Instant.now());
                throw new RefreshException("reuse_detected", "Refresh token reuse detected; all sessions revoked");
            }

            throw new RefreshException("revoked_refresh", "Refresh token has been revoked!");
        } else if(token.getRefreshTokenStatus() == RefreshTokenStatus.EXPIRED ||
                Instant.now().isAfter(token.getExpiresAt())) {
            token.setRefreshTokenStatus(RefreshTokenStatus.EXPIRED);
            refreshTokenRepository.save(token);
            throw new RefreshException("expired_refresh", "Refresh token expired!");
        }

        // Validate token version
        User user = userService.findByUserId(token.getUserId());
        if(!user.getTokenVersion().equals(token.getIssuedTokenVersion())) {
            refreshTokenRepository.revokeAllActiveByUserId(token.getUserId(), Instant.now());
            throw new RefreshException("token_version_mismatch", "Refresh token token version mismatch; all sessions revoked");
        }

        UserPrincipal userPrincipal = new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                user.getTokenVersion()
        );

        return rotate(userPrincipal, token, deviceDetails);
    }

    @Override
    public void revoke(String incomingRefreshToken) {
        String hashedRefreshToken = TokenUtil.covertToSha256Hash(incomingRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hashedRefreshToken)
                .orElseThrow(() -> new RefreshException("invalid_refresh", "Refresh token not found!"));

        if (token.getRefreshTokenStatus() != RefreshTokenStatus.REVOKED) {
            token.setRefreshTokenStatus(RefreshTokenStatus.REVOKED);
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        }
    }

    private RefreshTokenDetails rotate(UserPrincipal userPrincipal, RefreshToken existingRefreshToken,
                                DeviceDetails deviceDetails) {

        RefreshTokenDetails newRefreshTokenDetails = issue(userPrincipal, deviceDetails);

        // Revoke existing token.
        existingRefreshToken.setRefreshTokenStatus(RefreshTokenStatus.REVOKED);
        existingRefreshToken.setRevokedAt(Instant.now());
        existingRefreshToken.setReplacedByTokenHash(newRefreshTokenDetails.refreshToken().getTokenHash());
        refreshTokenRepository.save(existingRefreshToken);

        return newRefreshTokenDetails;
    }
}
