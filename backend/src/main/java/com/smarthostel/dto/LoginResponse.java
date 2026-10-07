package com.smarthostel.dto;

import java.time.Instant;

public record LoginResponse(String token, Instant expiresAt, Long userId, String username, String name, String role) {
}
