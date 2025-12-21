package com.app.thinknshare.user.dtos;

import java.util.List;
import java.util.UUID;

public record UserPrincipal(UUID userId, String username, String email, List<String> authorities, Integer tokenVersion) {
}
