package com.smarthostel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Student home page. Not cached: it is per-student and its statuses change often. */
public record StudentDashboardResponse(StudentResponse student, long openComplaints, long pendingLeaves,
                                       long upcomingVisitors, BigDecimal outstandingFees,
                                       List<ActivityItem> recentUpdates) {

    /** A recent status change shown as a notification. */
    public record ActivityItem(String type, Long id, String title, String status, LocalDateTime date) {
    }
}
