-- =====================================================================
-- 04_functions.sql : stored functions
-- =====================================================================

-- 1. Number of free beds in a room (capacity minus current occupancy).
--    Used by: vacancy screens, sp_allocate_room, available-room queries.
CREATE OR REPLACE FUNCTION fn_available_beds (p_room_id IN NUMBER)
RETURN NUMBER
IS
    v_capacity  room.capacity%TYPE;
    v_occupied  room.occupied_count%TYPE;
BEGIN
    SELECT capacity, occupied_count
      INTO v_capacity, v_occupied
      FROM room
     WHERE room_id = p_room_id;

    RETURN v_capacity - v_occupied;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20001, 'Room ' || p_room_id || ' does not exist');
END fn_available_beds;
/

-- 2. Occupancy percentage of a hostel (occupied beds / total beds * 100).
--    Used by: admin dashboard tiles and hostel reports.
CREATE OR REPLACE FUNCTION fn_hostel_occupancy_pct (p_hostel_id IN NUMBER)
RETURN NUMBER
IS
    v_total_beds     NUMBER;
    v_occupied_beds  NUMBER;
BEGIN
    SELECT NVL(SUM(capacity), 0), NVL(SUM(occupied_count), 0)
      INTO v_total_beds, v_occupied_beds
      FROM room
     WHERE hostel_id = p_hostel_id;

    IF v_total_beds = 0 THEN
        RETURN 0;
    END IF;

    RETURN ROUND(v_occupied_beds * 100 / v_total_beds, 2);
END fn_hostel_occupancy_pct;
/

-- 3. Number of complaints a maintenance staff member is currently working on.
--    Used by: complaint assignment screen (pick the least busy person)
--             and the auto-assign cursor procedure.
CREATE OR REPLACE FUNCTION fn_staff_open_workload (p_staff_id IN NUMBER)
RETURN NUMBER
IS
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM complaint
     WHERE assigned_to = p_staff_id
       AND status IN ('Assigned', 'In Progress');

    RETURN v_count;
END fn_staff_open_workload;
/

-- 4. Lowest bed number in a room that is not taken by an active allocation.
--    Returns NULL when the room is full. Used by sp_allocate_room.
CREATE OR REPLACE FUNCTION fn_find_free_bed (p_room_id IN NUMBER)
RETURN NUMBER
IS
    v_capacity room.capacity%TYPE;
    v_taken    NUMBER;
BEGIN
    SELECT capacity INTO v_capacity FROM room WHERE room_id = p_room_id;

    FOR bed IN 1 .. v_capacity LOOP
        SELECT COUNT(*)
          INTO v_taken
          FROM room_allocation
         WHERE room_id = p_room_id
           AND bed_number = bed
           AND status = 'Active';

        IF v_taken = 0 THEN
            RETURN bed;
        END IF;
    END LOOP;

    RETURN NULL;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RAISE_APPLICATION_ERROR(-20001, 'Room ' || p_room_id || ' does not exist');
END fn_find_free_bed;
/
