package com.smarthostel.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The single place where Java calls the Oracle stored procedures and functions.
 *
 * JdbcTemplate uses the same database connection and transaction as JPA, so when a
 * @Transactional service method calls these, everything commits or rolls back together.
 * If a procedure raises ORA-20xxx, Spring turns it into a DataAccessException, which
 * GlobalExceptionHandler returns to the frontend as HTTP 400 with the Oracle message.
 */
@Repository
@RequiredArgsConstructor
public class PlsqlRepository {

    private final JdbcTemplate jdbcTemplate;

    public record AllocationResult(long allocationId, int bedNumber) {
    }

    public record AutoAssignResult(int assigned, int skipped) {
    }

    // ---------------- procedures ----------------

    /** sp_allocate_room(student, room, staff, OUT allocation_id, OUT bed_number) */
    public AllocationResult allocateRoom(long studentId, long roomId, long staffId) {
        return jdbcTemplate.execute((ConnectionCallback<AllocationResult>) con -> {
            try (CallableStatement cs = con.prepareCall("{call sp_allocate_room(?, ?, ?, ?, ?)}")) {
                cs.setLong(1, studentId);
                cs.setLong(2, roomId);
                cs.setLong(3, staffId);
                cs.registerOutParameter(4, Types.NUMERIC);
                cs.registerOutParameter(5, Types.NUMERIC);
                cs.execute();
                return new AllocationResult(cs.getLong(4), cs.getInt(5));
            }
        });
    }

    /** sp_vacate_room(allocation_id) */
    public void vacateRoom(long allocationId) {
        jdbcTemplate.update("BEGIN sp_vacate_room(?); END;", allocationId);
    }

    /** sp_assign_complaint(complaint, staff, scheduled_date) */
    public void assignComplaint(long complaintId, long staffId, LocalDate scheduledDate) {
        jdbcTemplate.update("BEGIN sp_assign_complaint(?, ?, ?); END;",
                complaintId, staffId, scheduledDate == null ? null : Date.valueOf(scheduledDate));
    }

    /** sp_decide_leave(leave, staff, 'Approved' | 'Rejected') */
    public void decideLeave(long leaveId, long staffId, String decision) {
        jdbcTemplate.update("BEGIN sp_decide_leave(?, ?, ?); END;", leaveId, staffId, decision);
    }

    /** Cursor procedure sp_auto_assign_open_complaints(OUT assigned, OUT skipped) */
    public AutoAssignResult autoAssignOpenComplaints() {
        return jdbcTemplate.execute((ConnectionCallback<AutoAssignResult>) con -> {
            try (CallableStatement cs = con.prepareCall("{call sp_auto_assign_open_complaints(?, ?)}")) {
                cs.registerOutParameter(1, Types.NUMERIC);
                cs.registerOutParameter(2, Types.NUMERIC);
                cs.execute();
                return new AutoAssignResult(cs.getInt(1), cs.getInt(2));
            }
        });
    }

    /**
     * Cursor procedure sp_hostel_report prints its report with DBMS_OUTPUT.
     * We enable DBMS_OUTPUT, run the procedure and then read the printed lines back.
     */
    public List<String> hostelReport() {
        return jdbcTemplate.execute((ConnectionCallback<List<String>>) con -> {
            List<String> lines = new ArrayList<>();
            try (CallableStatement run = con.prepareCall("BEGIN DBMS_OUTPUT.ENABLE(NULL); sp_hostel_report; END;");
                 CallableStatement read = con.prepareCall("BEGIN DBMS_OUTPUT.GET_LINE(?, ?); END;")) {
                run.execute();
                read.registerOutParameter(1, Types.VARCHAR);
                read.registerOutParameter(2, Types.INTEGER);
                while (true) {
                    read.execute();
                    if (read.getInt(2) != 0) {      // status 1 = no more lines
                        break;
                    }
                    String line = read.getString(1);
                    lines.add(line == null ? "" : line);
                }
            }
            try (CallableStatement off = con.prepareCall("BEGIN DBMS_OUTPUT.DISABLE; END;")) {
                off.execute();
            }
            return lines;
        });
    }

    // ---------------- functions ----------------

    public int availableBeds(long roomId) {
        return jdbcTemplate.queryForObject("SELECT fn_available_beds(?) FROM dual", Integer.class, roomId);
    }

    public double hostelOccupancyPct(long hostelId) {
        return jdbcTemplate.queryForObject("SELECT fn_hostel_occupancy_pct(?) FROM dual", Double.class, hostelId);
    }

    public int staffOpenWorkload(long staffId) {
        return jdbcTemplate.queryForObject("SELECT fn_staff_open_workload(?) FROM dual", Integer.class, staffId);
    }
}
