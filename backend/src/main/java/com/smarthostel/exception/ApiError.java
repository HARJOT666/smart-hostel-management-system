package com.smarthostel.exception;

import java.time.LocalDateTime;

/** JSON body returned for every error response. */
public record ApiError(int status, String error, String message, LocalDateTime timestamp) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, LocalDateTime.now());
    }
}
