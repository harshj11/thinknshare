package com.app.thinknshare.user.dtos;

import jakarta.annotation.Nullable;

public record DeviceDetails(@Nullable String deviceId, String userAgent, String ipAddress) {
}
