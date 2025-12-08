package com.app.thinknshare.user.controller;

import com.app.thinknshare.user.dtos.AuthResponse;
import com.app.thinknshare.user.dtos.LoginRequest;
import com.app.thinknshare.user.dtos.RegisterUserRequest;
import com.app.thinknshare.user.entity.User;
import com.app.thinknshare.user.exception.RefreshException;
import com.app.thinknshare.user.repository.RefreshTokenRepository;
import com.app.thinknshare.user.service.JwtService;
import com.app.thinknshare.user.service.RefreshTokenService;
import com.app.thinknshare.user.service.UserService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final RefreshTokenService refreshService;

    private final RefreshTokenRepository refreshTokenRepository;

    private final CookieProps cookieProps;

    private final RefreshProps refreshProps;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken unauthenticatedUserToken = UsernamePasswordAuthenticationToken
                .unauthenticated(loginRequest.getUsername(), loginRequest.getPassword());

        Authentication authentication = null;

        try {
            authentication = authenticationManager.authenticate(unauthenticatedUserToken);
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
        String plainRefreshToken = refreshService.issue(
                user.getId(),
                null,
                request.getHeader("User-Agent"),
                request.getRemoteAddr());

        String refreshCookie = createRefreshTokenCookie(plainRefreshToken);

        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, refreshCookie)
                .body(new AuthResponse(
                        JwtService.getToken(authentication),
                        "Bearer",
                        refreshProps.getExpiresDays() * 24 * 60 * 60
                ));
    }

    @PostMapping
    public ResponseEntity<String> createUser(@Valid @RequestBody RegisterUserRequest userDetails) {

        userService.validateUserDetails(userDetails);

        String password = userDetails.getPassword();
        String hashedPassword = passwordEncoder.encode(password);

        userDetails.setPassword(hashedPassword);

        User newUser = new User();
        newUser.setUsername(userDetails.getUsername());
        newUser.setEmail(userDetails.getEmail());
        newUser.setPassword(userDetails.getPassword());

        userService.saveUser(newUser);

        return ResponseEntity.status(HttpStatus.CREATED).
                body("Given user details are successfully registered");

    }

    @PostMapping("/token/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshCookie) {
        if (refreshCookie == null || refreshCookie.isBlank())
            throw new RefreshException("missing_refresh", "No refresh token cookie present");

        // Rotate refresh token atomically and issue new access
        String newRefreshToken = refreshService.verifyAndRotate(refreshCookie);

        // Load user from rotated token linkage
        UUID userId = refreshService.getUserIdFromRefreshToken(newRefreshToken);
        User user = userService.findByUserId(userId);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername( ),
                null,
                user.getRoles().stream().map(SimpleGrantedAuthority::new).toList()
        );

        String accessToken = JwtService.getToken(authentication);
        String newRefreshCookie = createRefreshTokenCookie(newRefreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, newRefreshCookie)
                .body(new AuthResponse(accessToken, "Bearer", refreshProps.getExpiresDays() * 24 * 60 * 60));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@CookieValue(name = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshCookie) {
        if (refreshCookie != null && !refreshCookie.isBlank()) {
            refreshService.revoke(refreshCookie);
        }

        String clearedRefreshCookie = CookieUtil.clearCookie(refreshCookie,
                cookieProps.isSecure(), "/", cookieProps.getSameSite());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearedRefreshCookie)
                .body(Map.of("status", "logged_out"));
    }

    private String createRefreshTokenCookie(String refreshToken) {
        return CookieUtil.buildCookie(REFRESH_TOKEN_COOKIE_NAME,
                refreshToken,
                cookieProps.isSecure(),
                "/api/v1/user",
                cookieProps.getDomain(),
                cookieProps.getSameSite(),
                Duration.ofDays(refreshProps.getExpiresDays()));
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
