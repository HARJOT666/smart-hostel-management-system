package com.smarthostel.dto;

import com.smarthostel.model.Complaint;
import com.smarthostel.model.RoomAllocation;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ComplaintResponse(Long id, Long studentId, String studentName, String registrationNo,
                                String hostelName, String roomNumber,
                                String category, String description, String priority, String status,
                                LocalDateTime createdDate, LocalDateTime resolvedDate, String remarks,
                                LocalDate scheduledDate, Long assignedToId, String assignedToName,
                                String imageUrl, String completionImageUrl) {

    /** room = the student's active allocation (may be null) */
    public static ComplaintResponse from(Complaint c, RoomAllocation room) {
        return new ComplaintResponse(c.getId(), c.getStudent().getId(), c.getStudent().getName(),
                c.getStudent().getRegistrationNo(),
                room == null ? null : room.getRoom().getHostel().getHostelName(),
                room == null ? null : room.getRoom().getRoomNumber(),
                c.getCategory(), c.getDescription(), c.getPriority(), c.getStatus(), c.getCreatedDate(),
                c.getResolvedDate(), c.getRemarks(), c.getScheduledDate(),
                c.getAssignedTo() == null ? null : c.getAssignedTo().getId(),
                c.getAssignedTo() == null ? null : c.getAssignedTo().getName(),
                toUrl(c.getImagePath()), toUrl(c.getCompletionImagePath()));
    }

    static String toUrl(String path) {
        return path == null ? null : "/uploads/" + path;
    }
}
