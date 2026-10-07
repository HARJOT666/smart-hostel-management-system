package com.smarthostel.dto;

import jakarta.validation.constraints.*;

public record HostelRequest(
        @NotBlank @Size(max = 60) String hostelName,
        @NotBlank @Pattern(regexp = "Male|Female") String genderType,
        @NotBlank String location,
        @NotNull @Min(1) @Max(30) Integer totalFloors) {
}
