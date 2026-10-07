package com.smarthostel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Approve / reject a leave request or visitor pass. */
public record DecisionRequest(@NotBlank @Pattern(regexp = "Approved|Rejected") String decision) {
}
