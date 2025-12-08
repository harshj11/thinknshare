package com.app.thinknshare.user.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    com.app.thinknshare.user.util.CookieProps.class,
    com.app.thinknshare.user.util.RefreshProps.class
})
public class PropertiesConfig {
}
