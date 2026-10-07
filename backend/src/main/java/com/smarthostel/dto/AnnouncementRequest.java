package com.smarthostel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** hostelId = null means the notice is for every hostel. */
public record AnnouncementRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 2000) String body,
        @NotBlank @Pattern(regexp = "General|Maintenance|Mess|Event|Urgent") String category,
        Long hostelId,
        LocalDate expiryDate,
        boolean pinned) {
}
