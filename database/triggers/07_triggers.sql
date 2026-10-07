-- =====================================================================
-- 07_triggers.sql : automatic database actions
-- =====================================================================

-- 1. Validate a new allocation before it is saved:
--    the bed number must exist in the room (bed_number <= capacity).
CREATE OR REPLACE TRIGGER trg_allocation_validate
BEFORE INSERT ON room_allocation
FOR EACH ROW
DECLARE
    v_capacity room.capacity%TYPE;
BEGIN
    SELECT capacity INTO v_capacity FROM room WHERE room_id = :NEW.room_id;

    IF :NEW.bed_number > v_capacity THEN
        RAISE_APPLICATION_ERROR(-20016, 'Bed ' || :NEW.bed_number || ' does not exist in a room of capacity ' || v_capacity);
    END IF;
END;
/

-- 2. Keep ROOM.occupied_count in step with active allocations.
--    New active allocation -> +1, allocation leaves 'Active' -> -1.
--    If a room would go over capacity, the CHECK constraint ck_room_occupied
--    on ROOM rejects the update and the whole statement is rolled back.
CREATE OR REPLACE TRIGGER trg_allocation_occupancy
AFTER INSERT OR DELETE OR UPDATE OF status ON room_allocation
FOR EACH ROW
BEGIN
    IF INSERTING AND :NEW.status = 'Active' THEN
        UPDATE room SET occupied_count = occupied_count + 1 WHERE room_id = :NEW.room_id;

    ELSIF UPDATING AND :OLD.status = 'Active' AND :NEW.status <> 'Active' THEN
        UPDATE room SET occupied_count = occupied_count - 1 WHERE room_id = :OLD.room_id;

    ELSIF UPDATING AND :OLD.status <> 'Active' AND :NEW.status = 'Active' THEN
        UPDATE room SET occupied_count = occupied_count + 1 WHERE room_id = :NEW.room_id;

    ELSIF DELETING AND :OLD.status = 'Active' THEN
        UPDATE room SET occupied_count = occupied_count - 1 WHERE room_id = :OLD.room_id;
    END IF;
END;
/

-- 3. Complaint status rules:
--    * giving an Open complaint a staff member makes it 'Assigned'
--    * a complaint cannot be In Progress / Resolved without a staff member
--    * moving to 'Resolved' stamps resolved_date automatically
--    * moving away from 'Resolved' (re-opening) clears resolved_date
CREATE OR REPLACE TRIGGER trg_complaint_status
BEFORE INSERT OR UPDATE ON complaint
FOR EACH ROW
BEGIN
    IF :NEW.assigned_to IS NOT NULL AND :NEW.status = 'Open' THEN
        :NEW.status := 'Assigned';
    END IF;

    IF :NEW.status IN ('In Progress', 'Resolved') AND :NEW.assigned_to IS NULL THEN
        RAISE_APPLICATION_ERROR(-20050, 'Complaint must be assigned before it can be ' || :NEW.status);
    END IF;

    IF :NEW.status = 'Resolved' AND :NEW.resolved_date IS NULL THEN
        :NEW.resolved_date := SYSTIMESTAMP;
    ELSIF :NEW.status <> 'Resolved' THEN
        :NEW.resolved_date := NULL;
    END IF;
END;
/

-- 4. Generate a unique QR code value when a visitor pass is approved.
--    The frontend draws the QR image from this value; the gate verifies it.
CREATE OR REPLACE TRIGGER trg_visitor_pass_qr
BEFORE INSERT OR UPDATE OF status ON visitor_pass
FOR EACH ROW
BEGIN
    IF :NEW.status IN ('Approved', 'Completed') AND :NEW.qr_code IS NULL THEN
        :NEW.qr_code := 'SHMS-VP-' || :NEW.pass_id || '-' || SUBSTR(RAWTOHEX(SYS_GUID()), 1, 8);
    ELSIF :NEW.status IN ('Pending', 'Rejected') THEN
        :NEW.qr_code := NULL;
    END IF;
END;
/
