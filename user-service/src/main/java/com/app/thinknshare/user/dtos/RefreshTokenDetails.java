package com.app.thinknshare.user.dtos;

import com.app.thinknshare.user.entity.RefreshToken;

public record RefreshTokenDetails(RefreshToken refreshToken, String plainRefreshToken) {
}
