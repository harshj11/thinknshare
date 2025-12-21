package com.app.thinknshare.user.controller;

import com.app.thinknshare.user.dtos.*;
import com.app.thinknshare.user.entity.User;
import com.app.thinknshare.user.exception.RefreshException;
import com.app.thinknshare.user.service.AccessTokenService;
import com.app.thinknshare.user.service.IRefreshTokenService;
import com.app.thinknshare.user.service.UserService;
import com.app.thinknshare.user.util.ApplicationConstants;
import com.app.thinknshare.user.util.CookieProps;
import com.app.thinknshare.user.util.CookieUtil;
import com.app.thinknshare.user.util.RefreshProps;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;

    private final CookieProps cookieProps;

    private final RefreshProps refreshProps;

    private final AccessTokenService accessTokenService;

    private final IRefreshTokenService refreshTokenService;

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken unauthenticatedUserToken = UsernamePasswordAuthenticationToken
                .unauthenticated(loginRequest.getUsername(), loginRequest.getPassword());

        try {
            authenticationManager.authenticate(unauthenticatedUserToken);
        } catch (UsernameNotFoundException e) {
            LOG.warn("Login failed, user {} not found", loginRequest.getUsername());
            throw new BadCredentialsException("Invalid credentials");
        } catch (BadCredentialsException e) {
            LOG.warn("Login failed, bad credentials for user {}", loginRequest.getUsername());
            throw new BadCredentialsException("Invalid credentials");
        } catch (AuthenticationException e) {
            LOG.warn("Login failed: {}", e.getMessage());
            throw new BadCredentialsException("Invalid credentials");
        } catch(Exception e) {
            LOG.warn("Authentication failed: {}", e.getMessage());
            e.printStackTrace();
            return null;
        }

        User user = userService.findByUsername(loginRequest.getUsername());

        UserPrincipal userPrincipal = new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                user.getTokenVersion()
        );

        DeviceDetails deviceDetails = getDeviceDetails(request);

        String plainRefreshToken = refreshTokenService.issue(
                userPrincipal,
                deviceDetails
        ).plainRefreshToken();

        String refreshCookie = createRefreshTokenCookie(plainRefreshToken);

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, refreshCookie)
                .body(new AuthResponse(
                        accessTokenService.getAccessToken(userPrincipal, Map.of()),
                        "Bearer",
                        refreshProps.getExpiresDays() * 24 * 60 * 60
                ));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = ApplicationConstants.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshCookie,
            HttpServletRequest request
    ) {
        if (refreshCookie == null || refreshCookie.isBlank())
            throw new RefreshException("missing_refresh", "No refresh token cookie present");

        DeviceDetails deviceDetails = getDeviceDetails(request);

        RefreshTokenDetails refreshTokenDetails = refreshTokenService.verifyAndRotate(
                refreshCookie,
                deviceDetails
        );

        User user = userService.findByUserId(refreshTokenDetails.refreshToken().getUserId());

        UserPrincipal userPrincipal = new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles(),
                user.getTokenVersion()
        );

        String newAccessToken = accessTokenService.getAccessToken(userPrincipal, Map.of());
        String newRefreshCookie = createRefreshTokenCookie(refreshTokenDetails.plainRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newRefreshCookie)
                .body(new AuthResponse(
                        newAccessToken,
                        "Bearer",
                        refreshProps.getExpiresDays() * 24 * 60 * 60
                ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(name = ApplicationConstants.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshCookie
    ) {
        if (refreshCookie == null || refreshCookie.isBlank()) {
            throw new RefreshException("missing_refresh", "No refresh token cookie present");
        }

        refreshTokenService.revoke(refreshCookie);
        String clearedRefreshCookie = CookieUtil.clearCookie(refreshCookie,
                cookieProps.isSecure(), "/", cookieProps.getSameSite());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearedRefreshCookie)
                .body(Map.of("status", "logged_out"));
    }

    private String createRefreshTokenCookie(String refreshToken) {
        return CookieUtil.buildCookie(ApplicationConstants.REFRESH_TOKEN_COOKIE_NAME,
                refreshToken,
                cookieProps.isSecure(),
                "/api/v1/auth",
                cookieProps.getDomain(),
                cookieProps.getSameSite(),
                Duration.ofDays(refreshProps.getExpiresDays()));
    }

    private DeviceDetails getDeviceDetails(HttpServletRequest request) {
        return new DeviceDetails(
                null,
                request.getHeader("User-Agent"),
                request.getRemoteAddr()
        );
    }

    //    @PostMapping("/logout/all")
//    @PreAuthorize("isAuthenticated()")
//    public ResponseEntity<Map<String, String>> logoutAll(@AuthenticationPrincipal UserDetails userDetails) {
//        AppUser user = userRepo.findByEmail(userDetails.getUsername()).orElseThrow();
//        refreshService.revokeAllForUser(user.getId());
//        String cleared = cookieUtil.clearRefreshCookie();
//        return ResponseEntity.ok()
//                .header(HttpHeaders.SET_COOKIE, cleared)
//                .body(Map.of("status", "logged_out_everywhere"));
//    }
}
