package com.smarthostel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ComplaintRequest(
        @NotBlank @Pattern(regexp = "Electrical|Plumbing|Carpentry|Housekeeping|Network|Other") String category,
        @NotBlank @Size(min = 10, max = 1000) String description,
        @Pattern(regexp = "Low|Medium|High") String priority) {
}
