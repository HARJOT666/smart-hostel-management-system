package com.smarthostel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LostFoundRequest(
        @NotBlank @Pattern(regexp = "Lost|Found") String reportType,
        @NotBlank @Size(max = 80) String itemName,
        @Size(max = 500) String description,
        @Size(max = 100) String location) {
}
