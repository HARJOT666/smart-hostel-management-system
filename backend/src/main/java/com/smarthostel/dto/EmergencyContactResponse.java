package com.smarthostel.dto;

import com.smarthostel.model.Staff;

public record EmergencyContactResponse(String name, String role, String specialization, String phone, String email,
                                       String hostelName) {

    public static EmergencyContactResponse from(Staff s) {
        return new EmergencyContactResponse(s.getName(), s.getRole(), s.getSpecialization(), s.getPhone(),
                s.getEmail(), s.getHostel().getHostelName());
    }
}
