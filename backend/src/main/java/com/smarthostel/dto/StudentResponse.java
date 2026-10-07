package com.smarthostel.dto;

import com.smarthostel.model.RoomAllocation;
import com.smarthostel.model.Student;

import java.math.BigDecimal;

/** Student details plus the current room (null if the student is waiting for a room). */
public record StudentResponse(Long id, String registrationNo, String name, String email, String phone, String gender,
                              Integer year, String branch, BigDecimal cgpa,
                              Long allocationId, Long hostelId, String hostelName, String roomNumber, Integer bedNumber) {

    public static StudentResponse from(Student s, RoomAllocation active) {
        return new StudentResponse(s.getId(), s.getRegistrationNo(), s.getName(), s.getEmail(), s.getPhone(),
                s.getGender(), s.getYear(), s.getBranch(), s.getCgpa(),
                active == null ? null : active.getId(),
                active == null ? null : active.getRoom().getHostel().getId(),
                active == null ? null : active.getRoom().getHostel().getHostelName(),
                active == null ? null : active.getRoom().getRoomNumber(),
                active == null ? null : active.getBedNumber());
    }
}
