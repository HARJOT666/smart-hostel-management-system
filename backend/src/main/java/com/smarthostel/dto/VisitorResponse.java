package com.smarthostel.dto;

import com.smarthostel.model.VisitorPass;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VisitorResponse(Long id, Long studentId, String studentName, String registrationNo,
                              String visitorName, String visitorPhone, String relationToStudent, String purpose,
                              LocalDate visitDate, LocalDateTime entryTime, LocalDateTime exitTime,
                              String qrCode, String status, String approvedBy) {

    public static VisitorResponse from(VisitorPass v) {
        return new VisitorResponse(v.getId(), v.getStudent().getId(), v.getStudent().getName(),
                v.getStudent().getRegistrationNo(), v.getVisitorName(), v.getVisitorPhone(),
                v.getRelationToStudent(), v.getPurpose(), v.getVisitDate(), v.getEntryTime(), v.getExitTime(),
                v.getQrCode(), v.getStatus(), v.getApprovedBy() == null ? null : v.getApprovedBy().getName());
    }
}
