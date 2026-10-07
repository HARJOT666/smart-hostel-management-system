package com.smarthostel.dto;

import com.smarthostel.model.Staff;

public record StaffResponse(Long id, String name, String role, String specialization, String phone, String email,
                            Long hostelId, String hostelName) {

    public static StaffResponse from(Staff s) {
        return new StaffResponse(s.getId(), s.getName(), s.getRole(), s.getSpecialization(), s.getPhone(),
                s.getEmail(), s.getHostel().getId(), s.getHostel().getHostelName());
    }
}
