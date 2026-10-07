package com.smarthostel.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record AssignComplaintRequest(@NotNull Long staffId, LocalDate scheduledDate) {
}
