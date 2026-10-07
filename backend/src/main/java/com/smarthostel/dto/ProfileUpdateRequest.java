package com.smarthostel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** What a student may change on their own profile. */
public record ProfileUpdateRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{10}", message = "must be 10 digits") String phone) {
}
