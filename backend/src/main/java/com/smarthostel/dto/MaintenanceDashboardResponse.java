package com.smarthostel.dto;

import java.util.List;

public record MaintenanceDashboardResponse(String name, String specialization, String hostelName,
                                           long assigned, long inProgress, long resolvedThisMonth,
                                           long scheduledToday, List<ComplaintResponse> nextJobs) {
}
