package com.app.thinknshare.user.service;

import com.app.thinknshare.user.dtos.UserPrincipal;
import com.app.thinknshare.user.util.ApplicationConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class MyJwtService implements AccessTokenService {
    @Override
    public String getAccessToken(UserPrincipal userPrincipal, Map<String, Object> additionalClaims) {
        String secretValue = System.getenv()
                .getOrDefault(
                        ApplicationConstants.JWT_SECRET_KEY,
                        ApplicationConstants.DEFAULT_SECRET_KEY
                );

        SecretKey key = Keys.hmacShaKeyFor(secretValue.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .claim(ApplicationConstants.SUB, userPrincipal.userId().toString())
                .claim(ApplicationConstants.AUTHORITIES, userPrincipal.authorities())
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + 600000))
                .signWith(key).compact();
    }
}
