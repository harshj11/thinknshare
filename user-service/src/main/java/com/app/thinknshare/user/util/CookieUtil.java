package com.app.thinknshare.user.util;

import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class CookieUtil {

    public static String buildCookie(String cookieName,
                                     String cookieValue,
                                     boolean secure,
                                     String path,
                                     String domain,
                                     String sameSite,
                                     Duration maxAge) {
         ResponseCookie.ResponseCookieBuilder cookie = ResponseCookie.from(cookieName, cookieValue)
                 .httpOnly(true)
                 .secure(secure)
                 .path(path)
                 .maxAge(maxAge);

        if (domain != null && !domain.isBlank())
            cookie.domain(domain);
        // SameSite needs raw attribute:
        cookie.sameSite(sameSite);
        return cookie.build().toString();
    }

    public static String clearCookie(String cookieName,
                                    boolean secure,
                                    String path,
                                    String sameSite) {
        return ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .secure(secure)
            .path(path)
            .maxAge(Duration.ZERO)
            .sameSite(sameSite)
            .build().toString();
    }
}
