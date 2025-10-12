package com.app.thinknshare.user.exception;

import lombok.Data;

@Data
public class ErrorResponse {
    private int status;
    private String message;

    public ErrorResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
