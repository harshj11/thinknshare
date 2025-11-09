package com.app.thinknshare.user.service;

import com.app.thinknshare.user.util.ApplicationConstants;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.stream.Collectors;

public class JwtService {

    public static String getToken(Authentication authentication) {
        String secret = System.getenv().getOrDefault(ApplicationConstants.JWT_SECRET_KEY, ApplicationConstants.DEFAULT_SECRET_KEY);
        SecretKey secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder().claim(ApplicationConstants.USERNAME, authentication.getName())
                .claim(ApplicationConstants.AUTHORITIES, authentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.joining(","))
                )
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + 30000000))
                .signWith(secretKey).compact();
    }
}
