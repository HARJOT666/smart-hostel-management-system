package com.smarthostel.dto;

import com.smarthostel.model.LeaveRequest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record LeaveResponse(Long id, Long studentId, String studentName, String registrationNo,
                            LocalDate startDate, LocalDate endDate, long days, String reason, String destination,
                            LocalDate appliedDate, String status, String approvedBy) {

    public static LeaveResponse from(LeaveRequest l) {
        return new LeaveResponse(l.getId(), l.getStudent().getId(), l.getStudent().getName(),
                l.getStudent().getRegistrationNo(), l.getStartDate(), l.getEndDate(),
                ChronoUnit.DAYS.between(l.getStartDate(), l.getEndDate()) + 1,
                l.getReason(), l.getDestination(), l.getAppliedDate(), l.getStatus(),
                l.getApprovedBy() == null ? null : l.getApprovedBy().getName());
    }
}
