package com.smarthostel.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ListingRequest(
        @NotBlank @Size(max = 100) String title,
        @Size(max = 1000) String description,
        @NotBlank @Pattern(regexp = "Books|Electronics|Cycle|Furniture|Bedding|Other") String category,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        @NotBlank @Pattern(regexp = "New|Like New|Good|Fair") String itemCondition) {
}
