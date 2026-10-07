package com.smarthostel.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Admin/warden dashboard numbers. Cached in Redis for 60 seconds. */
public record AdminDashboardResponse(long totalStudents, long allocatedStudents, long waitingStudents,
                                     long totalBeds, long occupiedBeds, double occupancyPct,
                                     long openComplaints, long activeComplaints, long pendingLeaves,
                                     long pendingVisitors, long visitorsToday, BigDecimal outstandingFees,
                                     List<HostelOccupancy> hostels, List<NameCount> complaintsByStatus,
                                     List<NameCount> complaintsLast30Days, LocalDateTime generatedAt) {

    public record HostelOccupancy(Long hostelId, String hostelName, long beds, long occupied, double occupancyPct) {
    }

    public record NameCount(String name, long count) {
    }
}
