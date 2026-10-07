package com.smarthostel.dto;

import com.smarthostel.model.RoomAllocation;

import java.time.LocalDate;

public record AllocationResponse(Long id, Long studentId, String registrationNo, String studentName,
                                 Long roomId, String roomNumber, String roomType, Long hostelId, String hostelName,
                                 Integer bedNumber, LocalDate allocationDate, LocalDate vacateDate, String status,
                                 String allocatedBy) {

    public static AllocationResponse from(RoomAllocation a) {
        return new AllocationResponse(a.getId(), a.getStudent().getId(), a.getStudent().getRegistrationNo(),
                a.getStudent().getName(), a.getRoom().getId(), a.getRoom().getRoomNumber(), a.getRoom().getRoomType(),
                a.getRoom().getHostel().getId(), a.getRoom().getHostel().getHostelName(), a.getBedNumber(),
                a.getAllocationDate(), a.getVacateDate(), a.getStatus(), a.getAllocatedBy().getName());
    }
}
