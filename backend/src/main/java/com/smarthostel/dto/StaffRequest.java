package com.smarthostel.dto;

import jakarta.validation.constraints.*;

public record StaffRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "Admin|Warden|Maintenance") String role,
        @Pattern(regexp = "Electrical|Plumbing|Carpentry|Housekeeping|Network") String specialization,
        @NotBlank @Pattern(regexp = "\\d{10}", message = "must be 10 digits") String phone,
        @NotBlank @Email String email,
        @NotNull Long hostelId,
        String password) {
}
