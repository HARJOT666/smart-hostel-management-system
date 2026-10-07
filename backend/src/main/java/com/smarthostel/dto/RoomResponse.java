package com.smarthostel.dto;

import com.smarthostel.model.Room;

public record RoomResponse(Long id, String roomNumber, Integer floorNo, String roomType, Integer capacity,
                           Integer occupiedCount, Integer availableBeds, Long hostelId, String hostelName,
                           String genderType) {

    public static RoomResponse from(Room r) {
        return new RoomResponse(r.getId(), r.getRoomNumber(), r.getFloorNo(), r.getRoomType(), r.getCapacity(),
                r.getOccupiedCount(), r.getAvailableBeds(), r.getHostel().getId(), r.getHostel().getHostelName(),
                r.getHostel().getGenderType());
    }
}
