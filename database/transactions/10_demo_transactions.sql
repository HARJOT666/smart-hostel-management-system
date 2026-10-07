-- =====================================================================
-- 10_demo_transactions.sql : real-world transaction demonstrations
--
-- Each block shows: the operation, the tables involved, the SQL/PL-SQL
-- component executed, how the database changes, and the result.
-- Expected business-rule errors are caught and printed.
--
-- Everything is ROLLED BACK at the end, so the script can be re-run and
-- always starts from the same seeded data.
-- =====================================================================
SET SERVEROUTPUT ON SIZE UNLIMITED
SET LINESIZE 200

PROMPT
PROMPT ===== T1. ROOM ALLOCATION (sp_allocate_room + trg_allocation_validate + trg_allocation_occupancy) =====
DECLARE
    v_student   student%ROWTYPE;
    v_room      room%ROWTYPE;
    v_alloc_id  NUMBER;
    v_bed       NUMBER;
BEGIN
    -- a male student who is waiting for a room
    SELECT * INTO v_student FROM student s
     WHERE gender = 'Male'
       AND NOT EXISTS (SELECT 1 FROM room_allocation a WHERE a.student_id = s.student_id AND a.status = 'Active')
       AND ROWNUM = 1;
    -- a room in a male hostel with a free bed
    SELECT * INTO v_room FROM room r
     WHERE r.hostel_id = 1 AND fn_available_beds(r.room_id) > 0 AND r.capacity > 1 AND ROWNUM = 1;

    DBMS_OUTPUT.PUT_LINE('Student : ' || v_student.registration_no || ' ' || v_student.name);
    DBMS_OUTPUT.PUT_LINE('Room    : ' || v_room.room_number || ' (' || v_room.room_type || ') occupied BEFORE = '
                         || v_room.occupied_count || '/' || v_room.capacity);

    sp_allocate_room(v_student.student_id, v_room.room_id, 2, v_alloc_id, v_bed);

    SELECT * INTO v_room FROM room WHERE room_id = v_room.room_id;
    DBMS_OUTPUT.PUT_LINE('Result  : allocation_id = ' || v_alloc_id || ', bed = ' || v_bed);
    DBMS_OUTPUT.PUT_LINE('Room    : occupied AFTER  = ' || v_room.occupied_count || '/' || v_room.capacity
                         || '  (updated by trigger)');

    -- Business rule checks (each must fail)
    DBMS_OUTPUT.PUT_LINE(CHR(10) || 'Rule checks:');
    BEGIN
        sp_allocate_room(v_student.student_id, v_room.room_id, 2, v_alloc_id, v_bed);
    EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('  same student again    -> ' || SQLERRM);
    END;

    DECLARE v_female NUMBER;
    BEGIN
        SELECT student_id INTO v_female FROM student s WHERE gender = 'Female'
           AND NOT EXISTS (SELECT 1 FROM room_allocation a WHERE a.student_id = s.student_id AND a.status = 'Active')
           AND ROWNUM = 1;
        sp_allocate_room(v_female, v_room.room_id, 2, v_alloc_id, v_bed);
    EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('  female -> male hostel -> ' || SQLERRM);
    END;

    DECLARE v_full NUMBER; v_other NUMBER;
    BEGIN
        SELECT room_id INTO v_full FROM room WHERE hostel_id = 1 AND occupied_count = capacity AND ROWNUM = 1;
        SELECT student_id INTO v_other FROM student s WHERE gender = 'Male' AND student_id <> v_student.student_id
           AND NOT EXISTS (SELECT 1 FROM room_allocation a WHERE a.student_id = s.student_id AND a.status = 'Active')
           AND ROWNUM = 1;
        sp_allocate_room(v_other, v_full, 2, v_alloc_id, v_bed);
    EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('  room already full     -> ' || SQLERRM);
    END;

    DECLARE v_waiting NUMBER;
    BEGIN
        SELECT student_id INTO v_waiting FROM student s WHERE gender = 'Male' AND student_id <> v_student.student_id
           AND NOT EXISTS (SELECT 1 FROM room_allocation a WHERE a.student_id = s.student_id AND a.status = 'Active')
           AND ROWNUM = 1;
        sp_allocate_room(v_waiting, v_room.room_id, 3, v_alloc_id, v_bed);   -- staff 3 is Maintenance
    EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('  maintenance allocates -> ' || SQLERRM);
    END;
END;
/

PROMPT
PROMPT ===== T2. VACATE ROOM (sp_vacate_room + trg_allocation_occupancy) =====
DECLARE
    v_alloc  room_allocation%ROWTYPE;
    v_before NUMBER;
    v_after  NUMBER;
BEGIN
    SELECT * INTO v_alloc FROM room_allocation WHERE status = 'Active' AND student_id = 2;
    SELECT occupied_count INTO v_before FROM room WHERE room_id = v_alloc.room_id;

    sp_vacate_room(v_alloc.allocation_id);

    SELECT occupied_count INTO v_after FROM room WHERE room_id = v_alloc.room_id;
    DBMS_OUTPUT.PUT_LINE('Allocation ' || v_alloc.allocation_id || ' vacated. Room ' || v_alloc.room_id
                         || ' occupied: ' || v_before || ' -> ' || v_after);
    BEGIN
        sp_vacate_room(v_alloc.allocation_id);
    EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('Vacate again -> ' || SQLERRM);
    END;
END;
/

PROMPT
PROMPT ===== T3. COMPLAINT LIFECYCLE (INSERT -> sp_assign_complaint -> UPDATE, trg_complaint_status) =====
DECLARE
    v_id      NUMBER;
    v_c       complaint%ROWTYPE;
    v_plumber NUMBER;
    v_elec    NUMBER;
BEGIN
    INSERT INTO complaint (student_id, category, description, priority)
    VALUES (1, 'Electrical', 'Demo: ceiling fan regulator is not working.', 'High')
    RETURNING complaint_id INTO v_id;
    SELECT * INTO v_c FROM complaint WHERE complaint_id = v_id;
    DBMS_OUTPUT.PUT_LINE('Raised      : complaint ' || v_id || ' status = ' || v_c.status || ' (DEFAULT)');

    BEGIN
        UPDATE complaint SET status = 'In Progress' WHERE complaint_id = v_id;
    EXCEPTION WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Start work before assignment -> ' || REGEXP_SUBSTR(SQLERRM, '^[^' || CHR(10) || ']*'));
    END;

    SELECT staff_id INTO v_plumber FROM staff WHERE hostel_id = 1 AND specialization = 'Plumbing' AND ROWNUM = 1;
    BEGIN
        sp_assign_complaint(v_id, v_plumber);
    EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('Assign to plumber -> ' || SQLERRM);
    END;

    SELECT staff_id INTO v_elec FROM staff
     WHERE hostel_id = 1 AND specialization = 'Electrical'
     ORDER BY fn_staff_open_workload(staff_id) FETCH FIRST 1 ROW ONLY;
    sp_assign_complaint(v_id, v_elec, TRUNC(SYSDATE) + 1);
    SELECT * INTO v_c FROM complaint WHERE complaint_id = v_id;
    DBMS_OUTPUT.PUT_LINE('Assigned    : staff ' || v_c.assigned_to || ', status = ' || v_c.status
                         || ', scheduled ' || TO_CHAR(v_c.scheduled_date, 'DD-Mon-YYYY')
                         || ', staff workload now ' || fn_staff_open_workload(v_elec));

    UPDATE complaint SET status = 'In Progress' WHERE complaint_id = v_id;
    UPDATE complaint SET status = 'Resolved', remarks = 'Regulator replaced.' WHERE complaint_id = v_id;
    SELECT * INTO v_c FROM complaint WHERE complaint_id = v_id;
    DBMS_OUTPUT.PUT_LINE('Resolved    : status = ' || v_c.status || ', resolved_date set by trigger = '
                         || CASE WHEN v_c.resolved_date IS NOT NULL THEN 'YES' ELSE 'NO' END);
END;
/

PROMPT
PROMPT ===== T4. LEAVE APPROVAL (sp_decide_leave) =====
DECLARE
    v_leave  leave_request%ROWTYPE;
BEGIN
    SELECT * INTO v_leave FROM leave_request WHERE status = 'Pending' AND student_id = 1 AND ROWNUM = 1;
    DBMS_OUTPUT.PUT_LINE('Leave ' || v_leave.leave_id || ' (' || TO_CHAR(v_leave.start_date, 'DD-Mon') || ' to '
                         || TO_CHAR(v_leave.end_date, 'DD-Mon') || ', ' || v_leave.destination || ') status = ' || v_leave.status);
    BEGIN
        sp_decide_leave(v_leave.leave_id, 3, 'Approved');
    EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('Maintenance staff decides -> ' || SQLERRM);
    END;

    sp_decide_leave(v_leave.leave_id, 2, 'Approved');
    SELECT * INTO v_leave FROM leave_request WHERE leave_id = v_leave.leave_id;
    DBMS_OUTPUT.PUT_LINE('After warden decision: status = ' || v_leave.status || ', approved_by = ' || v_leave.approved_by);

    BEGIN
        sp_decide_leave(v_leave.leave_id, 2, 'Rejected');
    EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('Decide again -> ' || SQLERRM);
    END;
END;
/

PROMPT
PROMPT ===== T5. VISITOR PASS APPROVAL (UPDATE + trg_visitor_pass_qr) =====
DECLARE
    v_id  NUMBER;
    v_p   visitor_pass%ROWTYPE;
BEGIN
    INSERT INTO visitor_pass (student_id, visitor_name, visitor_phone, relation_to_student, purpose, visit_date)
    VALUES (1, 'Rakesh Mehta', '9800000001', 'Father', 'Weekend visit', TRUNC(SYSDATE) + 2)
    RETURNING pass_id INTO v_id;
    SELECT * INTO v_p FROM visitor_pass WHERE pass_id = v_id;
    DBMS_OUTPUT.PUT_LINE('Requested : pass ' || v_id || ' status = ' || v_p.status || ', qr_code = ' || NVL(v_p.qr_code, 'NULL'));

    UPDATE visitor_pass SET status = 'Approved', approved_by = 2 WHERE pass_id = v_id;
    SELECT * INTO v_p FROM visitor_pass WHERE pass_id = v_id;
    DBMS_OUTPUT.PUT_LINE('Approved  : qr_code = ' || v_p.qr_code || '  (generated by trigger)');

    UPDATE visitor_pass SET entry_time = SYSTIMESTAMP, exit_time = SYSTIMESTAMP + INTERVAL '2' HOUR,
                            status = 'Completed' WHERE pass_id = v_id;
    SELECT * INTO v_p FROM visitor_pass WHERE pass_id = v_id;
    DBMS_OUTPUT.PUT_LINE('Completed : entry ' || TO_CHAR(v_p.entry_time, 'HH24:MI') || ', exit '
                         || TO_CHAR(v_p.exit_time, 'HH24:MI') || ', qr_code kept = ' || v_p.qr_code);
END;
/

PROMPT
PROMPT ===== T6. AUTO-ASSIGN OPEN COMPLAINTS (cursor sp_auto_assign_open_complaints) =====
DECLARE
    v_open_before NUMBER;
    v_open_after  NUMBER;
    v_assigned    NUMBER;
    v_skipped     NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_open_before FROM complaint WHERE status = 'Open';
    sp_auto_assign_open_complaints(v_assigned, v_skipped);
    SELECT COUNT(*) INTO v_open_after FROM complaint WHERE status = 'Open';
    DBMS_OUTPUT.PUT_LINE('Open complaints: ' || v_open_before || ' -> ' || v_open_after
                         || '   (assigned ' || v_assigned || ', skipped ' || v_skipped || ')');
END;
/

PROMPT
PROMPT ===== T7. HOSTEL REPORT (cursor sp_hostel_report + fn_hostel_occupancy_pct) =====
EXEC sp_hostel_report;

PROMPT
PROMPT ===== T8. CONSTRAINT ENFORCEMENT =====
DECLARE
    PROCEDURE try (p_label VARCHAR2, p_sql VARCHAR2) IS
    BEGIN
        EXECUTE IMMEDIATE p_sql;
        DBMS_OUTPUT.PUT_LINE(RPAD(p_label, 42) || '-> accepted (unexpected!)');
    EXCEPTION WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE(RPAD(p_label, 42) || '-> ' || REGEXP_SUBSTR(SQLERRM, '^[^' || CHR(10) || ']*'));
    END;
BEGIN
    try('CHECK  cgpa = 11',
        q'[UPDATE student SET cgpa = 11 WHERE student_id = 1]');
    try('CHECK  room type vs capacity',
        q'[INSERT INTO room (room_number, floor_no, room_type, capacity, hostel_id) VALUES ('AR-999', 1, 'Single', 3, 1)]');
    try('FK     room in hostel 99',
        q'[INSERT INTO room (room_number, floor_no, room_type, capacity, hostel_id) VALUES ('XX-101', 1, 'Double', 2, 99)]');
    try('UNIQUE duplicate staff e-mail',
        q'[UPDATE staff SET email = 'admin@shms.local' WHERE staff_id = 2]');
    try('NOT NULL student name',
        q'[UPDATE student SET name = NULL WHERE student_id = 1]');
    try('CHECK  leave ends before start',
        q'[INSERT INTO leave_request (student_id, start_date, end_date, reason) VALUES (1, DATE '2026-11-10', DATE '2026-11-05', 'x')]');
    try('UNIQUE 2nd active allocation',
        q'[INSERT INTO room_allocation (student_id, room_id, allocated_by, bed_number) VALUES (1, 5, 1, 1)]');
    try('FK     delete room that has allocations',
        q'[DELETE FROM room WHERE room_id = (SELECT room_id FROM room_allocation WHERE student_id = 1 AND status = 'Active')]');
END;
/

PROMPT
PROMPT ===== T9. INSERT / UPDATE / DELETE on an extension table =====
DECLARE
    v_id NUMBER;
    v_n  NUMBER;
BEGIN
    INSERT INTO marketplace_listing (seller_id, title, category, price, item_condition)
    VALUES (1, 'Demo: Computer Networks textbook', 'Books', 300, 'Good')
    RETURNING listing_id INTO v_id;
    DBMS_OUTPUT.PUT_LINE('INSERT listing ' || v_id);
    UPDATE marketplace_listing SET price = 250 WHERE listing_id = v_id;
    DBMS_OUTPUT.PUT_LINE('UPDATE price -> ' || SQL%ROWCOUNT || ' row');
    DELETE FROM marketplace_listing WHERE listing_id = v_id;
    DBMS_OUTPUT.PUT_LINE('DELETE        -> ' || SQL%ROWCOUNT || ' row');
    SELECT COUNT(*) INTO v_n FROM marketplace_listing WHERE listing_id = v_id;
    DBMS_OUTPUT.PUT_LINE('Rows left with that id: ' || v_n);
END;
/

ROLLBACK;
PROMPT
PROMPT All demo changes rolled back.
