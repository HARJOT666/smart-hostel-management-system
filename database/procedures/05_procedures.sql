-- =====================================================================
-- 05_procedures.sql : stored procedures for the main business operations
--
-- The procedures do NOT commit. The caller decides:
--   * Spring Boot: the @Transactional service method commits or rolls back
--   * SQL*Plus demo scripts: an explicit COMMIT / ROLLBACK
-- Any rule violation raises an ORA-200xx error, which rolls back the work.
-- =====================================================================

-- 1. Allocate a room to a student.
--    Student -> verify student -> verify room -> verify gender & capacity
--    -> pick free bed -> insert allocation (trigger updates occupancy)
CREATE OR REPLACE PROCEDURE sp_allocate_room (
    p_student_id     IN  NUMBER,
    p_room_id        IN  NUMBER,
    p_staff_id       IN  NUMBER,
    p_allocation_id  OUT NUMBER,
    p_bed_number     OUT NUMBER
)
IS
    v_student_gender  student.gender%TYPE;
    v_hostel_gender   hostel.gender_type%TYPE;
    v_staff_role      staff.role%TYPE;
    v_active_count    NUMBER;
    v_dummy           NUMBER;
BEGIN
    -- verify student
    BEGIN
        SELECT gender INTO v_student_gender FROM student WHERE student_id = p_student_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20010, 'Student ' || p_student_id || ' does not exist');
    END;

    SELECT COUNT(*) INTO v_active_count
      FROM room_allocation
     WHERE student_id = p_student_id AND status = 'Active';
    IF v_active_count > 0 THEN
        RAISE_APPLICATION_ERROR(-20011, 'Student already has an active room allocation');
    END IF;

    -- verify staff member doing the allocation
    BEGIN
        SELECT role INTO v_staff_role FROM staff WHERE staff_id = p_staff_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20014, 'Staff ' || p_staff_id || ' does not exist');
    END;
    IF v_staff_role NOT IN ('Admin', 'Warden') THEN
        RAISE_APPLICATION_ERROR(-20014, 'Only an Admin or Warden can allocate rooms');
    END IF;

    -- verify room and lock its row, so two allocations cannot fill the same bed at once
    BEGIN
        SELECT r.room_id, h.gender_type
          INTO v_dummy, v_hostel_gender
          FROM room r JOIN hostel h ON h.hostel_id = r.hostel_id
         WHERE r.room_id = p_room_id
           FOR UPDATE OF r.occupied_count;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20012, 'Room ' || p_room_id || ' does not exist');
    END;

    IF v_hostel_gender <> v_student_gender THEN
        RAISE_APPLICATION_ERROR(-20013, 'Room is in a ' || v_hostel_gender || ' hostel; student is ' || v_student_gender);
    END IF;

    -- verify capacity
    IF fn_available_beds(p_room_id) <= 0 THEN
        RAISE_APPLICATION_ERROR(-20015, 'Room is full');
    END IF;

    p_bed_number := fn_find_free_bed(p_room_id);

    INSERT INTO room_allocation (student_id, room_id, allocated_by, bed_number, allocation_date, status)
    VALUES (p_student_id, p_room_id, p_staff_id, p_bed_number, SYSDATE, 'Active')
    RETURNING allocation_id INTO p_allocation_id;
    -- trg_allocation_occupancy increases room.occupied_count here
END sp_allocate_room;
/

-- 2. Vacate a room: mark the active allocation as Vacated.
--    trg_allocation_occupancy then frees the bed (occupied_count - 1).
CREATE OR REPLACE PROCEDURE sp_vacate_room (p_allocation_id IN NUMBER)
IS
    v_status room_allocation.status%TYPE;
BEGIN
    BEGIN
        SELECT status INTO v_status FROM room_allocation WHERE allocation_id = p_allocation_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20020, 'Allocation ' || p_allocation_id || ' does not exist');
    END;

    IF v_status <> 'Active' THEN
        RAISE_APPLICATION_ERROR(-20021, 'Allocation is already ' || v_status);
    END IF;

    UPDATE room_allocation
       SET status = 'Vacated',
           vacate_date = SYSDATE
     WHERE allocation_id = p_allocation_id;
END sp_vacate_room;
/

-- 3. Assign a complaint to a maintenance staff member.
--    Checks: complaint is still open/assigned, staff is Maintenance,
--    specialization matches the category ('Other' can go to anyone),
--    and staff works in the hostel where the student currently lives.
CREATE OR REPLACE PROCEDURE sp_assign_complaint (
    p_complaint_id    IN NUMBER,
    p_staff_id        IN NUMBER,
    p_scheduled_date  IN DATE DEFAULT NULL
)
IS
    v_status          complaint.status%TYPE;
    v_category        complaint.category%TYPE;
    v_student_id      complaint.student_id%TYPE;
    v_role            staff.role%TYPE;
    v_specialization  staff.specialization%TYPE;
    v_staff_hostel    staff.hostel_id%TYPE;
    v_student_hostel  hostel.hostel_id%TYPE;
BEGIN
    BEGIN
        SELECT status, category, student_id
          INTO v_status, v_category, v_student_id
          FROM complaint WHERE complaint_id = p_complaint_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20030, 'Complaint ' || p_complaint_id || ' does not exist');
    END;

    IF v_status NOT IN ('Open', 'Assigned') THEN
        RAISE_APPLICATION_ERROR(-20030, 'Complaint is already ' || v_status || ' and cannot be reassigned');
    END IF;

    BEGIN
        SELECT role, specialization, hostel_id
          INTO v_role, v_specialization, v_staff_hostel
          FROM staff WHERE staff_id = p_staff_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20031, 'Staff ' || p_staff_id || ' does not exist');
    END;

    IF v_role <> 'Maintenance' THEN
        RAISE_APPLICATION_ERROR(-20031, 'Complaints can only be assigned to maintenance staff');
    END IF;

    IF v_category <> 'Other' AND v_specialization <> v_category THEN
        RAISE_APPLICATION_ERROR(-20032, 'A ' || v_category || ' complaint cannot be assigned to a '
                                        || v_specialization || ' specialist');
    END IF;

    -- hostel check (only if the student currently has a room)
    BEGIN
        SELECT r.hostel_id INTO v_student_hostel
          FROM room_allocation a JOIN room r ON r.room_id = a.room_id
         WHERE a.student_id = v_student_id AND a.status = 'Active';

        IF v_student_hostel <> v_staff_hostel THEN
            RAISE_APPLICATION_ERROR(-20033, 'Staff member works in a different hostel');
        END IF;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN NULL;
    END;

    UPDATE complaint
       SET assigned_to    = p_staff_id,
           status         = 'Assigned',
           scheduled_date = NVL(p_scheduled_date, scheduled_date)
     WHERE complaint_id = p_complaint_id;
END sp_assign_complaint;
/

-- 4. Approve or reject a pending leave request.
CREATE OR REPLACE PROCEDURE sp_decide_leave (
    p_leave_id  IN NUMBER,
    p_staff_id  IN NUMBER,
    p_decision  IN VARCHAR2
)
IS
    v_status  leave_request.status%TYPE;
    v_role    staff.role%TYPE;
BEGIN
    IF p_decision NOT IN ('Approved', 'Rejected') THEN
        RAISE_APPLICATION_ERROR(-20040, 'Decision must be Approved or Rejected');
    END IF;

    BEGIN
        SELECT status INTO v_status FROM leave_request WHERE leave_id = p_leave_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20041, 'Leave request ' || p_leave_id || ' does not exist');
    END;

    IF v_status <> 'Pending' THEN
        RAISE_APPLICATION_ERROR(-20041, 'Leave request is already ' || v_status);
    END IF;

    BEGIN
        SELECT role INTO v_role FROM staff WHERE staff_id = p_staff_id;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RAISE_APPLICATION_ERROR(-20042, 'Staff ' || p_staff_id || ' does not exist');
    END;

    IF v_role NOT IN ('Warden', 'Admin') THEN
        RAISE_APPLICATION_ERROR(-20042, 'Only a Warden or Admin can decide leave requests');
    END IF;

    UPDATE leave_request
       SET status = p_decision,
           approved_by = p_staff_id
     WHERE leave_id = p_leave_id;
END sp_decide_leave;
/
