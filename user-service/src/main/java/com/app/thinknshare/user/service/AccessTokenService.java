package com.app.thinknshare.user.service;

import com.app.thinknshare.user.dtos.UserPrincipal;

import java.util.Map;

public interface AccessTokenService {
    String getAccessToken(UserPrincipal userPrincipal, Map<String, Object> additionalClaims);
}
