package com.smarthostel.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Student self-registration. */
public record RegisterRequest(
        @NotBlank @Pattern(regexp = "\\d{2}[A-Z]{3}\\d{4}", message = "must look like 24BYB1234") String registrationNo,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{10}", message = "must be 10 digits") String phone,
        @NotBlank @Pattern(regexp = "Male|Female") String gender,
        @NotNull @Min(1) @Max(4) Integer year,
        @NotBlank String branch,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal cgpa,
        @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password) {
}
