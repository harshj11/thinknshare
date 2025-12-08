package com.app.thinknshare.user.util;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.refresh")
@Getter
public class RefreshProps {
    private final long expiresDays = 30;
}
