package com.smarthostel.dto;

import jakarta.validation.constraints.NotNull;

public record AllocateRequest(@NotNull Long studentId, @NotNull Long roomId) {
}
