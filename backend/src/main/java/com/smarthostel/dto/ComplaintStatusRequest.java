package com.smarthostel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Maintenance staff updating their work. */
public record ComplaintStatusRequest(
        @NotBlank @Pattern(regexp = "In Progress|Resolved") String status,
        @Size(max = 500) String remarks,
        LocalDate scheduledDate) {
}
