package com.smarthostel.dto;

import com.smarthostel.model.LostFoundItem;

import java.time.LocalDate;

public record LostFoundResponse(Long id, Long reportedById, String reportedBy, String reporterPhone, String claimedBy,
                                String reportType, String itemName, String description, String location,
                                LocalDate reportDate, String status, String imageUrl) {

    public static LostFoundResponse from(LostFoundItem i) {
        return new LostFoundResponse(i.getId(), i.getReportedBy().getId(), i.getReportedBy().getName(),
                i.getReportedBy().getPhone(), i.getClaimedBy() == null ? null : i.getClaimedBy().getName(),
                i.getReportType(), i.getItemName(), i.getDescription(), i.getLocation(), i.getReportDate(),
                i.getStatus(), ComplaintResponse.toUrl(i.getImagePath()));
    }
}
