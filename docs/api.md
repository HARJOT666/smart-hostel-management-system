# REST API

Generated from the controller source by `tools/generate_api_doc.py`.

Base URL `http://localhost:8080`. Send `Authorization: Bearer <JWT>` except for public endpoints.
Errors are JSON: `{status, error, message, timestamp}` — 400 validation/business rule (incl. ORA-20xxx), 401 not logged in, 403 wrong role, 404 not found, 409 constraint violation, 429 too many failed logins.

| Module | Method | Path | Roles | Notes |
|---|---|---|---|---|
| Allocation | GET | `/api/allocations` | ADMIN,WARDEN |  |
| Allocation | POST | `/api/allocations` | ADMIN,WARDEN | Allocates a room by calling the Oracle procedure sp_allocate_room. |
| Allocation | PUT | `/api/allocations/{id}/vacate` | ADMIN,WARDEN | Vacates a room by calling the Oracle procedure sp_vacate_room. |
| Allocation | GET | `/api/allocations/my` | STUDENT |  |
| Announcement | GET | `/api/announcements` | any logged-in user | Students automatically get their own hostel's notices; staff can choose a hostel (or all). |
| Announcement | POST | `/api/announcements` | ADMIN,WARDEN |  |
| Announcement | PUT | `/api/announcements/{id}` | ADMIN,WARDEN |  |
| Announcement | DELETE | `/api/announcements/{id}` | ADMIN,WARDEN |  |
| Auth | POST | `/api/auth/login` | public | Public. Rate-limited per IP address (see LoginRateLimiter). |
| Auth | POST | `/api/auth/register` | public | Public. Student self-registration. |
| Auth | GET | `/api/auth/me` | any logged-in user | Who am I? (reads the user from the JWT) |
| Complaint | POST | `/api/complaints` | STUDENT |  |
| Complaint | POST | `/api/complaints/{id}/image` | STUDENT |  |
| Complaint | GET | `/api/complaints/my` | STUDENT |  |
| Complaint | GET | `/api/complaints` | ADMIN,WARDEN |  |
| Complaint | PUT | `/api/complaints/{id}/assign` | ADMIN,WARDEN | Calls sp_assign_complaint. |
| Complaint | POST | `/api/complaints/auto-assign` | ADMIN,WARDEN | Calls the cursor procedure sp_auto_assign_open_complaints. |
| Complaint | GET | `/api/complaints/assigned` | MAINTENANCE |  |
| Complaint | PUT | `/api/complaints/{id}/status` | MAINTENANCE |  |
| Complaint | POST | `/api/complaints/{id}/completion-image` | MAINTENANCE |  |
| Complaint | GET | `/api/complaints/{id}` | any logged-in user |  |
| Dashboard | GET | `/api/dashboard/admin` | ADMIN,WARDEN |  |
| Dashboard | GET | `/api/dashboard/student` | STUDENT |  |
| Dashboard | GET | `/api/dashboard/maintenance` | MAINTENANCE |  |
| Fee | GET | `/api/fees/my` | STUDENT |  |
| Fee | GET | `/api/fees` | ADMIN,WARDEN |  |
| Fee | PUT | `/api/fees/{id}/payment` | ADMIN,WARDEN |  |
| Hostel | GET | `/api/hostels/{id}` | any logged-in user |  |
| Hostel | POST | `/api/hostels` | ADMIN |  |
| Hostel | PUT | `/api/hostels/{id}` | ADMIN |  |
| Inventory | GET | `/api/inventory` | ADMIN,WARDEN,MAINTENANCE |  |
| Inventory | POST | `/api/inventory` | ADMIN,WARDEN |  |
| Inventory | PUT | `/api/inventory/{id}` | ADMIN,WARDEN |  |
| Inventory | DELETE | `/api/inventory/{id}` | ADMIN,WARDEN |  |
| Leave | POST | `/api/leaves` | STUDENT |  |
| Leave | GET | `/api/leaves/my` | STUDENT |  |
| Leave | DELETE | `/api/leaves/{id}` | STUDENT |  |
| Leave | GET | `/api/leaves` | ADMIN,WARDEN |  |
| Leave | PUT | `/api/leaves/{id}/decision` | ADMIN,WARDEN | Calls the Oracle procedure sp_decide_leave. |
| Leave | GET | `/api/leaves/{id}` | STUDENT,ADMIN,WARDEN | Used for the printable approval letter. |
| LostFound | POST | `/api/lost-found` | STUDENT |  |
| LostFound | PUT | `/api/lost-found/{id}/claim` | STUDENT |  |
| LostFound | PUT | `/api/lost-found/{id}/close` | STUDENT,ADMIN,WARDEN |  |
| LostFound | POST | `/api/lost-found/{id}/image` | STUDENT |  |
| Marketplace | GET | `/api/marketplace/my` | STUDENT |  |
| Marketplace | POST | `/api/marketplace` | STUDENT |  |
| Marketplace | PUT | `/api/marketplace/{id}` | STUDENT,ADMIN,WARDEN |  |
| Marketplace | PUT | `/api/marketplace/{id}/status` | STUDENT,ADMIN,WARDEN |  |
| Marketplace | POST | `/api/marketplace/{id}/image` | STUDENT,ADMIN,WARDEN |  |
| Marketplace | DELETE | `/api/marketplace/{id}` | STUDENT,ADMIN,WARDEN |  |
| MessMenu | PUT | `/api/mess-menu/{id}` | ADMIN,WARDEN |  |
| Report | GET | `/api/reports/hostel-cursor-report` | ADMIN,WARDEN | Output of the cursor procedure sp_hostel_report (read from DBMS_OUTPUT). |
| Report | GET | `/api/reports/{key}` | ADMIN,WARDEN |  |
| Room | GET | `/api/rooms/available` | ADMIN,WARDEN | Rooms a student of this gender can be allocated to (free beds only). |
| Room | GET | `/api/rooms/{id}` | ADMIN,WARDEN |  |
| Room | GET | `/api/rooms/{id}/occupants` | ADMIN,WARDEN |  |
| Room | POST | `/api/rooms` | ADMIN |  |
| Room | PUT | `/api/rooms/{id}` | ADMIN |  |
| Staff | GET | `/api/staff/maintenance-options` | ADMIN,WARDEN | Eligible maintenance staff for a complaint, with their current workload. |
| Staff | GET | `/api/staff/{id}` | ADMIN,WARDEN |  |
| Staff | POST | `/api/staff` | ADMIN |  |
| Staff | PUT | `/api/staff/{id}` | ADMIN |  |
| Staff | DELETE | `/api/staff/{id}` | ADMIN |  |
| Student | GET | `/api/students/me` | STUDENT |  |
| Student | PUT | `/api/students/me` | STUDENT |  |
| Student | GET | `/api/students` | ADMIN,WARDEN |  |
| Student | GET | `/api/students/waiting` | ADMIN,WARDEN |  |
| Student | GET | `/api/students/{id}` | ADMIN,WARDEN |  |
| Student | POST | `/api/students` | ADMIN,WARDEN |  |
| Student | PUT | `/api/students/{id}` | ADMIN,WARDEN |  |
| Student | DELETE | `/api/students/{id}` | ADMIN |  |
| Visitor | POST | `/api/visitors` | STUDENT |  |
| Visitor | GET | `/api/visitors/my` | STUDENT |  |
| Visitor | DELETE | `/api/visitors/{id}` | STUDENT |  |
| Visitor | GET | `/api/visitors` | ADMIN,WARDEN |  |
| Visitor | PUT | `/api/visitors/{id}/decision` | ADMIN,WARDEN | Approve/reject. On approval the Oracle trigger trg_visitor_pass_qr creates the QR code. |
| Visitor | POST | `/api/visitors/scan` | ADMIN,WARDEN | Gate check-in / check-out using the pass QR code. |
| Visitor | GET | `/api/visitors/{id}` | any logged-in user |  |
