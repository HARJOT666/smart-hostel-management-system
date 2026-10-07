package com.smarthostel.dto;

import jakarta.validation.constraints.*;

public record InventoryRequest(
        @NotNull Long hostelId,
        @NotBlank @Size(max = 60) String itemName,
        @NotBlank @Pattern(regexp = "Furniture|Electrical|Appliance|Bedding|Cleaning|Other") String category,
        @NotNull @Min(0) Integer quantityTotal,
        @NotNull @Min(0) Integer quantityDamaged) {
}
