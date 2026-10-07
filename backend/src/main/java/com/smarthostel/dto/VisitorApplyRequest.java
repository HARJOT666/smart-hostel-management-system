package com.smarthostel.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record VisitorApplyRequest(
        @NotBlank @Size(max = 100) String visitorName,
        @NotBlank @Pattern(regexp = "\\d{10}", message = "must be 10 digits") String visitorPhone,
        @NotBlank @Size(max = 30) String relationToStudent,
        @Size(max = 200) String purpose,
        @NotNull @FutureOrPresent LocalDate visitDate) {
}
