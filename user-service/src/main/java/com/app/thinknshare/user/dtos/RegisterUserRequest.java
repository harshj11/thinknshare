package com.app.thinknshare.user.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterUserRequest {
    @NotBlank(message = "Username must not be empty")
    private String username;

    @NotBlank(message = "Email must not be empty")
    private String email;

    @NotBlank(message = "Password must not be empty")
    private String password;
}
