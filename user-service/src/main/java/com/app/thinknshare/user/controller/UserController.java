package com.app.thinknshare.user.controller;

import com.app.thinknshare.user.dtos.AuthResponse;
import com.app.thinknshare.user.dtos.LoginRequest;
import com.app.thinknshare.user.dtos.RegisterUserRequest;
import com.app.thinknshare.user.entity.User;
import com.app.thinknshare.user.service.JwtService;
import com.app.thinknshare.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken unauthenticatedUserToken = UsernamePasswordAuthenticationToken
                .unauthenticated(loginRequest.getUsername(), loginRequest.getPassword());

        try {
            Authentication authentication = authenticationManager.authenticate(unauthenticatedUserToken);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new AuthResponse(JwtService.getToken(authentication)));
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
}
