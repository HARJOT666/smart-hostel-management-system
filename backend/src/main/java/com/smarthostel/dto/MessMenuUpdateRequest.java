package com.smarthostel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessMenuUpdateRequest(@NotBlank @Size(max = 300) String menuItems) {
}
