package com.app.thinknshare.user.util;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.cookie")
@Getter
public class CookieProps {
    private final boolean secure = true;

    private String domain = "";

    private String sameSite="Strict";
}