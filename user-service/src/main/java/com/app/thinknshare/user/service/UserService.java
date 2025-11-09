package com.app.thinknshare.user.service;

import com.app.thinknshare.user.dtos.RegisterUserRequest;
import com.app.thinknshare.user.entity.User;
import com.app.thinknshare.user.exception.EmailAlreadyTaken;
import com.app.thinknshare.user.exception.UserNotFoundException;
import com.app.thinknshare.user.exception.UsernameAlreadyTakenException;
import com.app.thinknshare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User with username " + username + " not found"));
    }

    public User saveUser(User user) {
        return userRepository.save(user);
    }

    public void validateUserDetails(RegisterUserRequest userDetails) {
        userRepository.findByEmail(userDetails.getEmail())
                .ifPresent(foundUser -> {
                    throw new EmailAlreadyTaken("Email already registered!");
                });

        userRepository.findByUsername(userDetails.getUsername())
                .ifPresent(foundUser -> {
                    throw new UsernameAlreadyTakenException("Username is already taken!");
                });
    }
}