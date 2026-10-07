package com.smarthostel.service;

import com.smarthostel.config.CacheNames;
import com.smarthostel.dto.*;
import com.smarthostel.dto.AdminDashboardResponse.HostelOccupancy;
import com.smarthostel.dto.AdminDashboardResponse.NameCount;
import com.smarthostel.dto.StudentDashboardResponse.ActivityItem;
import com.smarthostel.model.Complaint;
import com.smarthostel.model.Staff;
import com.smarthostel.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ReportRepository reportRepository;
    private final StudentService studentService;
    private final ComplaintRepository complaintRepository;
    private final LeaveRequestRepository leaveRepository;
    private final VisitorPassRepository visitorRepository;
    private final FeePaymentRepository feeRepository;
    private final StaffRepository staffRepository;
    private final ComplaintService complaintService;

    /**
     * Admin dashboard statistics. Several aggregate queries -> cached for only 60 seconds
     * (shms:dashboardStats::admin). Also cleared whenever a room is allocated or vacated.
     * Screens that need exact live values (rooms, allocations) do not use this cache.
     */
    @Cacheable(value = CacheNames.DASHBOARD_STATS, key = "'admin'")
    @Transactional(readOnly = true)
    public AdminDashboardResponse adminDashboard() {
        log.info("CACHE MISS: computing admin dashboard statistics in Oracle");
        Map<String, Object> t = reportRepository.dashboardTotals();
        long totalStudents = num(t.get("TOTAL_STUDENTS"));
        long allocated = num(t.get("ALLOCATED_STUDENTS"));
        long beds = num(t.get("TOTAL_BEDS"));
        long occupied = num(t.get("OCCUPIED_BEDS"));

        // per-hostel occupancy; the percentage comes from the Oracle function fn_hostel_occupancy_pct
        List<HostelOccupancy> hostels = reportRepository.hostelOccupancy().stream()
                .map(r -> new HostelOccupancy(num(r.get("HOSTEL_ID")), (String) r.get("HOSTEL_NAME"),
                        num(r.get("BEDS")), num(r.get("OCCUPIED")), ((Number) r.get("OCCUPANCY_PCT")).doubleValue()))
                .toList();

        return new AdminDashboardResponse(totalStudents, allocated, totalStudents - allocated, beds, occupied,
                beds == 0 ? 0 : Math.round(occupied * 1000.0 / beds) / 10.0,
                num(t.get("OPEN_COMPLAINTS")), num(t.get("ACTIVE_COMPLAINTS")), num(t.get("PENDING_LEAVES")),
                num(t.get("PENDING_VISITORS")), num(t.get("VISITORS_TODAY")), (BigDecimal) t.get("OUTSTANDING_FEES"),
                hostels, toNameCounts(reportRepository.complaintsByStatus(), "STATUS"),
                toNameCounts(reportRepository.complaintsLast30Days(), "CATEGORY"), LocalDateTime.now());
    }

    /** Student home page - never cached (per student, live statuses). */
    @Transactional(readOnly = true)
    public StudentDashboardResponse studentDashboard(Long studentId) {
        StudentResponse student = studentService.getStudent(studentId);
        List<ActivityItem> updates = new ArrayList<>();
        complaintRepository.findByStudent(studentId).forEach(c -> updates.add(new ActivityItem("Complaint", c.getId(),
                c.getCategory() + " complaint", c.getStatus(),
                c.getResolvedDate() != null ? c.getResolvedDate() : c.getCreatedDate())));
        leaveRepository.findByStudent(studentId).forEach(l -> updates.add(new ActivityItem("Leave", l.getId(),
                "Leave " + l.getStartDate() + " to " + l.getEndDate(), l.getStatus(), l.getAppliedDate().atStartOfDay())));
        visitorRepository.findByStudent(studentId).forEach(v -> updates.add(new ActivityItem("Visitor", v.getId(),
                "Visit by " + v.getVisitorName() + " on " + v.getVisitDate(), v.getStatus(), v.getVisitDate().atStartOfDay())));
        updates.sort(Comparator.comparing(ActivityItem::date).reversed());

        long openComplaints = complaintRepository.countByStudentIdAndStatusNot(studentId, "Resolved");
        long pendingLeaves = updates.stream().filter(u -> u.type().equals("Leave") && u.status().equals("Pending")).count();
        long upcomingVisitors = visitorRepository.findByStudent(studentId).stream()
                .filter(v -> !v.getVisitDate().isBefore(LocalDate.now()) && !"Rejected".equals(v.getStatus())).count();
        BigDecimal outstanding = feeRepository.findByStudent(studentId).stream()
                .map(f -> f.getAmountDue().subtract(f.getAmountPaid()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StudentDashboardResponse(student, openComplaints, pendingLeaves, upcomingVisitors, outstanding,
                updates.stream().limit(8).toList());
    }

    /** Maintenance home page - never cached. */
    @Transactional(readOnly = true)
    public MaintenanceDashboardResponse maintenanceDashboard(Long staffId) {
        Staff staff = staffRepository.findById(staffId).orElseThrow();
        List<Complaint> mine = complaintRepository.findByAssignedStaff(staffId, null);
        LocalDate today = LocalDate.now();
        long assigned = mine.stream().filter(c -> c.getStatus().equals("Assigned")).count();
        long inProgress = mine.stream().filter(c -> c.getStatus().equals("In Progress")).count();
        long resolvedThisMonth = mine.stream().filter(c -> c.getResolvedDate() != null
                && c.getResolvedDate().toLocalDate().withDayOfMonth(1).equals(today.withDayOfMonth(1))).count();
        long scheduledToday = mine.stream().filter(c -> !c.getStatus().equals("Resolved")
                && today.equals(c.getScheduledDate())).count();
        List<ComplaintResponse> next = complaintService.assignedTo(staffId, null).stream()
                .filter(c -> !c.status().equals("Resolved")).limit(5).toList();
        return new MaintenanceDashboardResponse(staff.getName(), staff.getSpecialization(),
                staff.getHostel().getHostelName(), assigned, inProgress, resolvedThisMonth, scheduledToday, next);
    }

    private static List<NameCount> toNameCounts(List<Map<String, Object>> rows, String nameColumn) {
        return rows.stream().map(r -> new NameCount((String) r.get(nameColumn), num(r.get("TOTAL")))).toList();
    }

    private static long num(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }
}
