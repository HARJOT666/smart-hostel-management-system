package com.smarthostel.dto;

import com.smarthostel.model.Announcement;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AnnouncementResponse(Long id, String title, String body, String category, String postedBy,
                                   Long hostelId, String hostelName, LocalDateTime postedDate, LocalDate expiryDate,
                                   boolean pinned) {

    public static AnnouncementResponse from(Announcement a) {
        return new AnnouncementResponse(a.getId(), a.getTitle(), a.getBody(), a.getCategory(),
                a.getPostedBy().getName(),
                a.getHostel() == null ? null : a.getHostel().getId(),
                a.getHostel() == null ? "All hostels" : a.getHostel().getHostelName(),
                a.getPostedDate(), a.getExpiryDate(), Character.valueOf('Y').equals(a.getIsPinned()));
    }
}
