-- =====================================================================
-- 02_core_constraints.sql : FOREIGN KEY, UNIQUE and CHECK constraints
-- for the 8 core tables, plus supporting indexes.
-- =====================================================================

-- ---------- HOSTEL ----------
ALTER TABLE hostel ADD CONSTRAINT uq_hostel_name   UNIQUE (hostel_name);
ALTER TABLE hostel ADD CONSTRAINT ck_hostel_gender CHECK (gender_type IN ('Male', 'Female'));
ALTER TABLE hostel ADD CONSTRAINT ck_hostel_floors CHECK (total_floors > 0);

-- ---------- STUDENT ----------
ALTER TABLE student ADD CONSTRAINT uq_student_regno  UNIQUE (registration_no);
ALTER TABLE student ADD CONSTRAINT uq_student_email  UNIQUE (email);
ALTER TABLE student ADD CONSTRAINT ck_student_gender CHECK (gender IN ('Male', 'Female'));
ALTER TABLE student ADD CONSTRAINT ck_student_year   CHECK (year BETWEEN 1 AND 4);
ALTER TABLE student ADD CONSTRAINT ck_student_cgpa   CHECK (cgpa BETWEEN 0 AND 10);

-- ---------- ROOM ----------
ALTER TABLE room ADD CONSTRAINT fk_room_hostel FOREIGN KEY (hostel_id) REFERENCES hostel (hostel_id);
ALTER TABLE room ADD CONSTRAINT uq_room_number UNIQUE (hostel_id, room_number);
ALTER TABLE room ADD CONSTRAINT ck_room_floor  CHECK (floor_no >= 0);
ALTER TABLE room ADD CONSTRAINT ck_room_type   CHECK (room_type IN ('Single', 'Double', 'Triple', 'Quad'));
ALTER TABLE room ADD CONSTRAINT ck_room_capacity CHECK (capacity > 0);
ALTER TABLE room ADD CONSTRAINT ck_room_occupied CHECK (occupied_count BETWEEN 0 AND capacity);
-- capacity must agree with the room type
ALTER TABLE room ADD CONSTRAINT ck_room_type_capacity CHECK (
       (room_type = 'Single' AND capacity = 1)
    OR (room_type = 'Double' AND capacity = 2)
    OR (room_type = 'Triple' AND capacity = 3)
    OR (room_type = 'Quad'   AND capacity = 4));

-- ---------- STAFF ----------
ALTER TABLE staff ADD CONSTRAINT fk_staff_hostel FOREIGN KEY (hostel_id) REFERENCES hostel (hostel_id);
ALTER TABLE staff ADD CONSTRAINT uq_staff_phone  UNIQUE (phone);
ALTER TABLE staff ADD CONSTRAINT uq_staff_email  UNIQUE (email);
ALTER TABLE staff ADD CONSTRAINT ck_staff_role   CHECK (role IN ('Admin', 'Warden', 'Maintenance'));
ALTER TABLE staff ADD CONSTRAINT ck_staff_spec   CHECK (specialization IN
    ('Electrical', 'Plumbing', 'Carpentry', 'Housekeeping', 'Network'));
-- maintenance staff must have a specialization; admins/wardens must not
ALTER TABLE staff ADD CONSTRAINT ck_staff_role_spec CHECK (
       (role = 'Maintenance' AND specialization IS NOT NULL)
    OR (role <> 'Maintenance' AND specialization IS NULL));

-- ---------- ROOM_ALLOCATION ----------
ALTER TABLE room_allocation ADD CONSTRAINT fk_alloc_student FOREIGN KEY (student_id)   REFERENCES student (student_id);
ALTER TABLE room_allocation ADD CONSTRAINT fk_alloc_room    FOREIGN KEY (room_id)      REFERENCES room (room_id);
ALTER TABLE room_allocation ADD CONSTRAINT fk_alloc_staff   FOREIGN KEY (allocated_by) REFERENCES staff (staff_id);
ALTER TABLE room_allocation ADD CONSTRAINT ck_alloc_bed     CHECK (bed_number > 0);
ALTER TABLE room_allocation ADD CONSTRAINT ck_alloc_dates   CHECK (vacate_date >= allocation_date);
ALTER TABLE room_allocation ADD CONSTRAINT ck_alloc_status  CHECK (status IN ('Active', 'Vacated', 'Cancelled'));

-- At most ONE active allocation per student, and ONE active allocation per (room, bed).
-- Function-based unique indexes: rows that are not 'Active' produce NULL keys and are ignored,
-- so the allocation history (Vacated / Cancelled rows) is still allowed.
CREATE UNIQUE INDEX ux_alloc_active_student ON room_allocation (
    CASE WHEN status = 'Active' THEN student_id END);
CREATE UNIQUE INDEX ux_alloc_active_bed ON room_allocation (
    CASE WHEN status = 'Active' THEN room_id END,
    CASE WHEN status = 'Active' THEN bed_number END);

-- ---------- COMPLAINT ----------
ALTER TABLE complaint ADD CONSTRAINT fk_complaint_student FOREIGN KEY (student_id)  REFERENCES student (student_id);
ALTER TABLE complaint ADD CONSTRAINT fk_complaint_staff   FOREIGN KEY (assigned_to) REFERENCES staff (staff_id);
ALTER TABLE complaint ADD CONSTRAINT ck_complaint_category CHECK (category IN
    ('Electrical', 'Plumbing', 'Carpentry', 'Housekeeping', 'Network', 'Other'));
ALTER TABLE complaint ADD CONSTRAINT ck_complaint_priority CHECK (priority IN ('Low', 'Medium', 'High'));
ALTER TABLE complaint ADD CONSTRAINT ck_complaint_status   CHECK (status IN ('Open', 'Assigned', 'In Progress', 'Resolved'));
ALTER TABLE complaint ADD CONSTRAINT ck_complaint_resolved CHECK (resolved_date >= created_date);

-- ---------- LEAVE_REQUEST ----------
ALTER TABLE leave_request ADD CONSTRAINT fk_leave_student FOREIGN KEY (student_id)  REFERENCES student (student_id);
ALTER TABLE leave_request ADD CONSTRAINT fk_leave_staff   FOREIGN KEY (approved_by) REFERENCES staff (staff_id);
ALTER TABLE leave_request ADD CONSTRAINT ck_leave_dates   CHECK (end_date >= start_date);
ALTER TABLE leave_request ADD CONSTRAINT ck_leave_status  CHECK (status IN ('Pending', 'Approved', 'Rejected'));

-- ---------- VISITOR_PASS ----------
ALTER TABLE visitor_pass ADD CONSTRAINT fk_visitor_student FOREIGN KEY (student_id)  REFERENCES student (student_id);
ALTER TABLE visitor_pass ADD CONSTRAINT fk_visitor_staff   FOREIGN KEY (approved_by) REFERENCES staff (staff_id);
ALTER TABLE visitor_pass ADD CONSTRAINT uq_visitor_qr      UNIQUE (qr_code);
ALTER TABLE visitor_pass ADD CONSTRAINT ck_visitor_times   CHECK (exit_time > entry_time);
ALTER TABLE visitor_pass ADD CONSTRAINT ck_visitor_status  CHECK (status IN ('Pending', 'Approved', 'Rejected', 'Completed'));

-- ---------- Indexes on foreign keys / frequently filtered columns ----------
CREATE INDEX ix_room_hostel        ON room (hostel_id);
CREATE INDEX ix_staff_hostel       ON staff (hostel_id);
CREATE INDEX ix_alloc_student      ON room_allocation (student_id);
CREATE INDEX ix_alloc_room         ON room_allocation (room_id);
CREATE INDEX ix_complaint_student  ON complaint (student_id);
CREATE INDEX ix_complaint_assigned ON complaint (assigned_to);
CREATE INDEX ix_complaint_status   ON complaint (status);
CREATE INDEX ix_leave_student      ON leave_request (student_id);
CREATE INDEX ix_leave_status       ON leave_request (status);
CREATE INDEX ix_visitor_student    ON visitor_pass (student_id);
CREATE INDEX ix_visitor_status     ON visitor_pass (status);
