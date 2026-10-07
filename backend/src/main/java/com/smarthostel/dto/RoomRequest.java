package com.smarthostel.dto;

import jakarta.validation.constraints.*;

public record RoomRequest(
        @NotNull Long hostelId,
        @NotBlank @Size(max = 10) String roomNumber,
        @NotNull @Min(0) Integer floorNo,
        @NotBlank @Pattern(regexp = "Single|Double|Triple|Quad") String roomType) {

    /** Capacity follows from the room type (same rule as CHECK ck_room_type_capacity). */
    public int capacity() {
        return switch (roomType) {
            case "Single" -> 1;
            case "Double" -> 2;
            case "Triple" -> 3;
            default -> 4;
        };
    }
}
