package com.smarthostel.dto;

import jakarta.validation.constraints.NotBlank;

/** username = registration number (students) or e-mail (staff) */
public record LoginRequest(@NotBlank String username, @NotBlank String password) {
}
