package com.smarthostel.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Report queries (taken from database/queries/09_queries.sql) run directly with JdbcTemplate.
 * Each report returns rows as maps: column name -> value.
 */
@Repository
@RequiredArgsConstructor
public class ReportRepository {

    private final JdbcTemplate jdbcTemplate;

    /** report key -> { title, SQL } */
    public static final Map<String, String[]> REPORTS = new LinkedHashMap<>();

    static {
        REPORTS.put("hostel-occupancy", new String[]{"Hostel occupancy", """
                SELECT h.hostel_name, h.gender_type, COUNT(r.room_id) AS rooms,
                       SUM(r.capacity) AS beds, SUM(r.occupied_count) AS occupied,
                       SUM(r.capacity) - SUM(r.occupied_count) AS vacant,
                       fn_hostel_occupancy_pct(h.hostel_id) AS occupancy_pct
                  FROM hostel h JOIN room r ON r.hostel_id = h.hostel_id
                 GROUP BY h.hostel_id, h.hostel_name, h.gender_type
                 ORDER BY h.hostel_id"""});
        REPORTS.put("complaints-by-category", new String[]{"Complaints by category", """
                SELECT category, COUNT(*) AS total,
                       SUM(CASE WHEN status = 'Resolved' THEN 1 ELSE 0 END) AS resolved,
                       SUM(CASE WHEN status <> 'Resolved' THEN 1 ELSE 0 END) AS pending,
                       ROUND(AVG(CASE WHEN status = 'Resolved'
                                      THEN CAST(resolved_date AS DATE) - CAST(created_date AS DATE) END), 2) AS avg_days_to_resolve
                  FROM complaint
                 GROUP BY category
                 ORDER BY total DESC"""});
        REPORTS.put("staff-workload", new String[]{"Maintenance staff workload", """
                SELECT st.name AS staff_name, st.specialization, h.hostel_name,
                       COUNT(c.complaint_id) AS total_handled,
                       SUM(CASE WHEN c.status = 'Resolved' THEN 1 ELSE 0 END) AS resolved,
                       fn_staff_open_workload(st.staff_id) AS open_now
                  FROM staff st
                  JOIN hostel h ON h.hostel_id = st.hostel_id
                  LEFT JOIN complaint c ON c.assigned_to = st.staff_id
                 WHERE st.role = 'Maintenance'
                 GROUP BY st.staff_id, st.name, st.specialization, h.hostel_name
                 ORDER BY open_now DESC, total_handled DESC"""});
        REPORTS.put("room-type-utilisation", new String[]{"Room type utilisation", """
                SELECT room_type, COUNT(*) AS rooms, SUM(capacity) AS beds, SUM(occupied_count) AS occupied,
                       ROUND(100 * SUM(occupied_count) / SUM(capacity), 1) AS utilisation_pct
                  FROM room
                 GROUP BY room_type
                 ORDER BY MIN(capacity)"""});
        REPORTS.put("leave-summary", new String[]{"Leave decisions by warden", """
                SELECT st.name AS warden, h.hostel_name, COUNT(*) AS decided,
                       SUM(CASE WHEN l.status = 'Approved' THEN 1 ELSE 0 END) AS approved,
                       SUM(CASE WHEN l.status = 'Rejected' THEN 1 ELSE 0 END) AS rejected,
                       ROUND(100 * SUM(CASE WHEN l.status = 'Approved' THEN 1 ELSE 0 END) / COUNT(*), 1) AS approval_pct
                  FROM leave_request l
                  JOIN staff st ON st.staff_id = l.approved_by
                  JOIN hostel h ON h.hostel_id = st.hostel_id
                 GROUP BY st.name, h.hostel_name
                 ORDER BY decided DESC"""});
        REPORTS.put("visitors-monthly", new String[]{"Visitor passes per month", """
                SELECT TO_CHAR(visit_date, 'YYYY-MM') AS month, COUNT(*) AS requests,
                       SUM(CASE WHEN status IN ('Approved', 'Completed') THEN 1 ELSE 0 END) AS approved,
                       SUM(CASE WHEN status = 'Rejected' THEN 1 ELSE 0 END) AS rejected,
                       SUM(CASE WHEN status = 'Pending' THEN 1 ELSE 0 END) AS pending
                  FROM visitor_pass
                 GROUP BY TO_CHAR(visit_date, 'YYYY-MM')
                 ORDER BY month"""});
        REPORTS.put("fee-defaulters", new String[]{"Outstanding fees (current semester)", """
                SELECT s.registration_no, s.name, SUM(f.amount_due) AS due, SUM(f.amount_paid) AS paid,
                       SUM(f.amount_due - f.amount_paid) AS outstanding
                  FROM fee_payment f JOIN student s ON s.student_id = f.student_id
                 WHERE f.semester = '2026-27 Odd'
                 GROUP BY s.registration_no, s.name
                HAVING SUM(f.amount_due - f.amount_paid) > 0
                 ORDER BY outstanding DESC"""});
        REPORTS.put("waiting-students", new String[]{"Students waiting for a room", """
                SELECT s.registration_no, s.name, s.gender, s.year, s.branch
                  FROM student s
                 WHERE NOT EXISTS (SELECT 1 FROM room_allocation a
                                    WHERE a.student_id = s.student_id AND a.status = 'Active')
                 ORDER BY s.year, s.registration_no"""});
    }

    public List<Map<String, Object>> runReport(String sql) {
        return jdbcTemplate.queryForList(sql);
    }

    /** All admin dashboard numbers in one round trip. */
    public Map<String, Object> dashboardTotals() {
        return jdbcTemplate.queryForMap("""
                SELECT (SELECT COUNT(*) FROM student) AS total_students,
                       (SELECT COUNT(*) FROM room_allocation WHERE status = 'Active') AS allocated_students,
                       (SELECT SUM(capacity) FROM room) AS total_beds,
                       (SELECT SUM(occupied_count) FROM room) AS occupied_beds,
                       (SELECT COUNT(*) FROM complaint WHERE status = 'Open') AS open_complaints,
                       (SELECT COUNT(*) FROM complaint WHERE status IN ('Assigned', 'In Progress')) AS active_complaints,
                       (SELECT COUNT(*) FROM leave_request WHERE status = 'Pending') AS pending_leaves,
                       (SELECT COUNT(*) FROM visitor_pass WHERE status = 'Pending') AS pending_visitors,
                       (SELECT COUNT(*) FROM visitor_pass
                         WHERE visit_date = TRUNC(SYSDATE) AND status IN ('Approved', 'Completed')) AS visitors_today,
                       (SELECT NVL(SUM(amount_due - amount_paid), 0) FROM fee_payment) AS outstanding_fees
                  FROM dual""");
    }

    public List<Map<String, Object>> hostelOccupancy() {
        return jdbcTemplate.queryForList("""
                SELECT h.hostel_id, h.hostel_name, SUM(r.capacity) AS beds, SUM(r.occupied_count) AS occupied,
                       fn_hostel_occupancy_pct(h.hostel_id) AS occupancy_pct
                  FROM hostel h JOIN room r ON r.hostel_id = h.hostel_id
                 GROUP BY h.hostel_id, h.hostel_name
                 ORDER BY h.hostel_id""");
    }

    public List<Map<String, Object>> complaintsByStatus() {
        return jdbcTemplate.queryForList("SELECT status, COUNT(*) AS total FROM complaint GROUP BY status");
    }

    public List<Map<String, Object>> complaintsLast30Days() {
        return jdbcTemplate.queryForList("""
                SELECT category, COUNT(*) AS total FROM complaint
                 WHERE created_date >= SYSTIMESTAMP - INTERVAL '30' DAY
                 GROUP BY category ORDER BY total DESC""");
    }
}
