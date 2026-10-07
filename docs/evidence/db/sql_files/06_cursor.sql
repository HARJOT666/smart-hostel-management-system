-- =====================================================================
-- 06_cursor.sql : cursor-based procedures (process many rows one by one)
-- =====================================================================

-- 1. Auto-assign all open, unassigned complaints.
--    An explicit cursor walks through the open complaints (High priority
--    and oldest first). For each one it finds the least busy maintenance
--    staff member with the right specialization in the student's hostel
--    (using fn_staff_open_workload) and calls sp_assign_complaint.
--    Used by the admin "Auto-assign" button.
CREATE OR REPLACE PROCEDURE sp_auto_assign_open_complaints (
    p_assigned_count  OUT NUMBER,
    p_skipped_count   OUT NUMBER
)
IS
    CURSOR c_open_complaints IS
        SELECT c.complaint_id, c.category,
               (SELECT r.hostel_id
                  FROM room_allocation a JOIN room r ON r.room_id = a.room_id
                 WHERE a.student_id = c.student_id AND a.status = 'Active') AS hostel_id
          FROM complaint c
         WHERE c.status = 'Open'
           AND c.assigned_to IS NULL
         ORDER BY CASE c.priority WHEN 'High' THEN 1 WHEN 'Medium' THEN 2 ELSE 3 END,
                  c.created_date;

    v_complaint  c_open_complaints%ROWTYPE;
    v_staff_id   staff.staff_id%TYPE;
BEGIN
    p_assigned_count := 0;
    p_skipped_count  := 0;

    OPEN c_open_complaints;
    LOOP
        FETCH c_open_complaints INTO v_complaint;
        EXIT WHEN c_open_complaints%NOTFOUND;

        BEGIN
            SELECT staff_id INTO v_staff_id
              FROM (SELECT s.staff_id
                      FROM staff s
                     WHERE s.role = 'Maintenance'
                       AND (v_complaint.category = 'Other' OR s.specialization = v_complaint.category)
                       AND (v_complaint.hostel_id IS NULL OR s.hostel_id = v_complaint.hostel_id)
                     ORDER BY fn_staff_open_workload(s.staff_id), s.staff_id)
             WHERE ROWNUM = 1;

            sp_assign_complaint(v_complaint.complaint_id, v_staff_id);
            p_assigned_count := p_assigned_count + 1;
            DBMS_OUTPUT.PUT_LINE('Complaint ' || v_complaint.complaint_id || ' (' || v_complaint.category
                                 || ') -> staff ' || v_staff_id);
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                p_skipped_count := p_skipped_count + 1;
                DBMS_OUTPUT.PUT_LINE('Complaint ' || v_complaint.complaint_id
                                     || ' skipped: no matching staff available');
        END;
    END LOOP;
    CLOSE c_open_complaints;
END sp_auto_assign_open_complaints;
/

-- 2. Hostel occupancy and complaint report (parameterised cursor inside a cursor FOR loop).
--    Prints one block per hostel with room-type breakdown, occupancy % and pending complaints.
CREATE OR REPLACE PROCEDURE sp_hostel_report
IS
    CURSOR c_hostels IS
        SELECT hostel_id, hostel_name, gender_type FROM hostel ORDER BY hostel_id;

    CURSOR c_room_types (p_hostel_id NUMBER) IS
        SELECT room_type, COUNT(*) AS rooms, SUM(capacity) AS beds, SUM(occupied_count) AS occupied
          FROM room
         WHERE hostel_id = p_hostel_id
         GROUP BY room_type
         ORDER BY MIN(capacity);

    v_pending NUMBER;
BEGIN
    DBMS_OUTPUT.PUT_LINE('===== SMART HOSTEL OCCUPANCY REPORT (' || TO_CHAR(SYSDATE, 'DD-Mon-YYYY') || ') =====');

    FOR h IN c_hostels LOOP
        SELECT COUNT(*) INTO v_pending
          FROM complaint c
          JOIN room_allocation a ON a.student_id = c.student_id AND a.status = 'Active'
          JOIN room r ON r.room_id = a.room_id
         WHERE r.hostel_id = h.hostel_id
           AND c.status <> 'Resolved';

        DBMS_OUTPUT.PUT_LINE(CHR(10) || h.hostel_name || ' (' || h.gender_type || ')  occupancy: '
                             || fn_hostel_occupancy_pct(h.hostel_id) || '%   pending complaints: ' || v_pending);

        FOR t IN c_room_types(h.hostel_id) LOOP
            DBMS_OUTPUT.PUT_LINE('   ' || RPAD(t.room_type, 8) || ' rooms: ' || LPAD(t.rooms, 3)
                                 || '   beds: ' || LPAD(t.beds, 4) || '   occupied: ' || LPAD(t.occupied, 4)
                                 || '   free: ' || LPAD(t.beds - t.occupied, 4));
        END LOOP;
    END LOOP;
END sp_hostel_report;
/
