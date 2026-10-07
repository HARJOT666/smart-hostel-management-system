-- =====================================================================
-- 09_queries.sql : business queries of the Smart Hostel Management System
-- Q1-Q18 retrieve data (read-only). Q19-Q21 show INSERT / UPDATE / DELETE
-- and are rolled back at the end so the script can be re-run.
-- =====================================================================
SET LINESIZE 200
SET PAGESIZE 60
COLUMN hostel_name FORMAT A16
COLUMN name        FORMAT A22
COLUMN branch      FORMAT A32
COLUMN category    FORMAT A13
COLUMN staff_name  FORMAT A20
COLUMN student     FORMAT A22
COLUMN room_number FORMAT A8
COLUMN destination FORMAT A14
COLUMN visitor_name FORMAT A18

PROMPT
PROMPT Q1. Students currently living in Aravali Block (4-table join, sorted by room)
SELECT s.registration_no, s.name, r.room_number, a.bed_number, a.allocation_date
  FROM student s
  JOIN room_allocation a ON a.student_id = s.student_id AND a.status = 'Active'
  JOIN room r            ON r.room_id = a.room_id
  JOIN hostel h          ON h.hostel_id = r.hostel_id
 WHERE h.hostel_name = 'Aravali Block'
 ORDER BY r.room_number, a.bed_number
 FETCH FIRST 12 ROWS ONLY;

PROMPT
PROMPT Q2. Rooms with free beds in each hostel (filter + function call)
SELECT h.hostel_name, r.room_number, r.room_type, r.capacity, r.occupied_count,
       fn_available_beds(r.room_id) AS free_beds
  FROM room r JOIN hostel h ON h.hostel_id = r.hostel_id
 WHERE r.occupied_count < r.capacity
 ORDER BY free_beds DESC, h.hostel_name, r.room_number
 FETCH FIRST 12 ROWS ONLY;

PROMPT
PROMPT Q3. Occupancy of every hostel (GROUP BY + aggregate + function)
SELECT h.hostel_name, h.gender_type, COUNT(r.room_id) AS rooms,
       SUM(r.capacity) AS beds, SUM(r.occupied_count) AS occupied,
       SUM(r.capacity) - SUM(r.occupied_count) AS vacant,
       fn_hostel_occupancy_pct(h.hostel_id) AS occupancy_pct
  FROM hostel h JOIN room r ON r.hostel_id = h.hostel_id
 GROUP BY h.hostel_id, h.hostel_name, h.gender_type
 ORDER BY occupancy_pct DESC;

PROMPT
PROMPT Q4. Complaints by category and status (GROUP BY on two columns)
SELECT category,
       COUNT(*) AS total,
       SUM(CASE WHEN status = 'Resolved' THEN 1 ELSE 0 END) AS resolved,
       SUM(CASE WHEN status <> 'Resolved' THEN 1 ELSE 0 END) AS pending
  FROM complaint
 GROUP BY category
 ORDER BY total DESC;

PROMPT
PROMPT Q5. Categories with average resolution time above 2 days (GROUP BY + HAVING)
SELECT category, COUNT(*) AS resolved_count,
       ROUND(AVG(CAST(resolved_date AS DATE) - CAST(created_date AS DATE)), 2) AS avg_days_to_resolve
  FROM complaint
 WHERE status = 'Resolved'
 GROUP BY category
HAVING AVG(CAST(resolved_date AS DATE) - CAST(created_date AS DATE)) > 2
 ORDER BY avg_days_to_resolve DESC;

PROMPT
PROMPT Q6. Unresolved complaints older than 3 days with student, room and assigned staff (5-table join, outer join)
SELECT c.complaint_id, c.category, c.priority, c.status,
       TRUNC(SYSDATE - CAST(c.created_date AS DATE)) AS days_open,
       s.name AS student, r.room_number, st.name AS staff_name
  FROM complaint c
  JOIN student s              ON s.student_id = c.student_id
  JOIN room_allocation a      ON a.student_id = s.student_id AND a.status = 'Active'
  JOIN room r                 ON r.room_id = a.room_id
  LEFT JOIN staff st          ON st.staff_id = c.assigned_to
 WHERE c.status <> 'Resolved'
   AND c.created_date < SYSTIMESTAMP - INTERVAL '3' DAY
 ORDER BY days_open DESC
 FETCH FIRST 12 ROWS ONLY;

PROMPT
PROMPT Q7. Maintenance staff workload (LEFT JOIN + GROUP BY + function)
SELECT st.staff_id, st.name AS staff_name, st.specialization, h.hostel_name,
       COUNT(c.complaint_id) AS total_handled,
       SUM(CASE WHEN c.status = 'Resolved' THEN 1 ELSE 0 END) AS resolved,
       fn_staff_open_workload(st.staff_id) AS open_now
  FROM staff st
  JOIN hostel h          ON h.hostel_id = st.hostel_id
  LEFT JOIN complaint c  ON c.assigned_to = st.staff_id
 WHERE st.role = 'Maintenance'
 GROUP BY st.staff_id, st.name, st.specialization, h.hostel_name
 ORDER BY open_now DESC, total_handled DESC
 FETCH FIRST 12 ROWS ONLY;

PROMPT
PROMPT Q8. Pending leave requests waiting for a warden (join + filter + sort)
SELECT l.leave_id, s.registration_no, s.name, h.hostel_name,
       l.start_date, l.end_date, l.end_date - l.start_date + 1 AS days, l.destination
  FROM leave_request l
  JOIN student s          ON s.student_id = l.student_id
  JOIN room_allocation a  ON a.student_id = s.student_id AND a.status = 'Active'
  JOIN room r             ON r.room_id = a.room_id
  JOIN hostel h           ON h.hostel_id = r.hostel_id
 WHERE l.status = 'Pending'
 ORDER BY l.start_date
 FETCH FIRST 12 ROWS ONLY;

PROMPT
PROMPT Q9. Leave approval rate of each warden (GROUP BY + HAVING)
SELECT st.name AS staff_name, h.hostel_name,
       COUNT(*) AS decided,
       SUM(CASE WHEN l.status = 'Approved' THEN 1 ELSE 0 END) AS approved,
       ROUND(100 * SUM(CASE WHEN l.status = 'Approved' THEN 1 ELSE 0 END) / COUNT(*), 1) AS approval_pct
  FROM leave_request l
  JOIN staff st  ON st.staff_id = l.approved_by
  JOIN hostel h  ON h.hostel_id = st.hostel_id
 GROUP BY st.name, h.hostel_name
HAVING COUNT(*) >= 10
 ORDER BY decided DESC;

PROMPT
PROMPT Q10. Visitors expected in the next 7 days with approved passes
SELECT v.pass_id, v.visit_date, v.visitor_name, v.relation_to_student, s.name AS student, v.qr_code
  FROM visitor_pass v JOIN student s ON s.student_id = v.student_id
 WHERE v.status = 'Approved'
   AND v.visit_date BETWEEN TRUNC(SYSDATE) AND TRUNC(SYSDATE) + 7
 ORDER BY v.visit_date, v.visitor_name;

PROMPT
PROMPT Q11. Visitor passes per month and status (date grouping)
SELECT TO_CHAR(visit_date, 'YYYY-MM') AS month,
       COUNT(*) AS requests,
       SUM(CASE WHEN status IN ('Approved', 'Completed') THEN 1 ELSE 0 END) AS approved,
       SUM(CASE WHEN status = 'Rejected' THEN 1 ELSE 0 END) AS rejected,
       SUM(CASE WHEN status = 'Pending' THEN 1 ELSE 0 END) AS pending
  FROM visitor_pass
 GROUP BY TO_CHAR(visit_date, 'YYYY-MM')
 ORDER BY month;

PROMPT
PROMPT Q12. Students waiting for a room (NOT EXISTS subquery)
SELECT s.registration_no, s.name, s.gender, s.year, s.branch
  FROM student s
 WHERE NOT EXISTS (SELECT 1 FROM room_allocation a
                    WHERE a.student_id = s.student_id AND a.status = 'Active')
 ORDER BY s.year, s.registration_no
 FETCH FIRST 12 ROWS ONLY;

PROMPT
PROMPT Q13. Students who raised more complaints than the average student (nested subquery in HAVING)
SELECT s.registration_no, s.name, COUNT(c.complaint_id) AS complaints
  FROM student s JOIN complaint c ON c.student_id = s.student_id
 GROUP BY s.registration_no, s.name
HAVING COUNT(c.complaint_id) > (SELECT AVG(cnt) FROM (SELECT COUNT(*) AS cnt FROM complaint GROUP BY student_id))
 ORDER BY complaints DESC
 FETCH FIRST 12 ROWS ONLY;

PROMPT
PROMPT Q14. Rooms that are completely full in the hostel with the highest occupancy (subquery in WHERE)
SELECT h.hostel_name, r.room_number, r.room_type, r.occupied_count || '/' || r.capacity AS occupancy
  FROM room r JOIN hostel h ON h.hostel_id = r.hostel_id
 WHERE r.occupied_count = r.capacity
   AND r.hostel_id = (SELECT hostel_id FROM hostel
                       ORDER BY fn_hostel_occupancy_pct(hostel_id) DESC FETCH FIRST 1 ROW ONLY)
 ORDER BY r.room_number;

PROMPT
PROMPT Q15. Room-type utilisation across all hostels (GROUP BY + ratio)
SELECT room_type, COUNT(*) AS rooms, SUM(capacity) AS beds, SUM(occupied_count) AS occupied,
       ROUND(100 * SUM(occupied_count) / SUM(capacity), 1) AS utilisation_pct
  FROM room
 GROUP BY room_type
 ORDER BY MIN(capacity);

PROMPT
PROMPT Q16. Students with outstanding fees for the current semester (extension table, join + aggregate)
SELECT s.registration_no, s.name,
       SUM(f.amount_due) AS due, SUM(f.amount_paid) AS paid,
       SUM(f.amount_due - f.amount_paid) AS outstanding
  FROM fee_payment f JOIN student s ON s.student_id = f.student_id
 WHERE f.semester = '2026-27 Odd'
 GROUP BY s.registration_no, s.name
HAVING SUM(f.amount_due - f.amount_paid) > 0
 ORDER BY outstanding DESC
 FETCH FIRST 12 ROWS ONLY;

PROMPT
PROMPT Q17. Hostel staff directory / emergency contacts (wardens and maintenance per hostel)
SELECT h.hostel_name, st.role, NVL(st.specialization, '-') AS specialization, st.name AS staff_name, st.phone
  FROM staff st JOIN hostel h ON h.hostel_id = st.hostel_id
 WHERE h.hostel_id = 1
 ORDER BY CASE st.role WHEN 'Warden' THEN 1 WHEN 'Admin' THEN 2 ELSE 3 END, st.specialization;

PROMPT
PROMPT Q18. Branch-wise residents and average CGPA of current residents (IN subquery + GROUP BY)
SELECT branch, COUNT(*) AS residents, ROUND(AVG(cgpa), 2) AS avg_cgpa
  FROM student
 WHERE student_id IN (SELECT student_id FROM room_allocation WHERE status = 'Active')
 GROUP BY branch
 ORDER BY residents DESC;

PROMPT
PROMPT ===== DML (rolled back at the end) =====
PROMPT Q19. INSERT a new student registration
INSERT INTO student (registration_no, name, email, phone, gender, year, branch, cgpa, password_hash)
VALUES ('26BYB1999', 'Demo New Student', 'demo.new@student.shms.local', '9000000099', 'Male', 1,
        'CSE (Cyber Security)', 8.10, '$2b$10$demoHashOnlyForThisInsertxxxxxxxxxxxxxxxxxxxxxxxxxxxx');
SELECT student_id, registration_no, name, year FROM student WHERE registration_no = '26BYB1999';

PROMPT Q20. UPDATE: warden reschedules all Assigned complaints of Aravali Block to tomorrow
UPDATE complaint c
   SET scheduled_date = TRUNC(SYSDATE) + 1
 WHERE c.status = 'Assigned'
   AND c.assigned_to IN (SELECT staff_id FROM staff WHERE hostel_id = 1);

PROMPT Q21. DELETE: remove withdrawn marketplace listings older than 30 days
DELETE FROM marketplace_listing
 WHERE status = 'Withdrawn'
   AND listed_date < SYSDATE - 30;

ROLLBACK;
