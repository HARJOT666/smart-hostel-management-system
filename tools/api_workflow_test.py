"""
End-to-end test of the main hostel workflows through the REAL REST API
(React would make exactly these calls): Spring Boot -> Oracle (SQL, procedures, triggers).

Run with the backend started:   python3 tools/api_workflow_test.py
Output is plain text; every line shows the real HTTP status and data returned.
This script CHANGES data (it allocates a room, raises a complaint, etc.).
Use database/reset-db.sh to go back to the seeded data.
"""
import json
import sys
import urllib.error
import urllib.request
from datetime import date

API = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8080/api"
PASSWORD = "Hostel@123"
failures = []


def call(method, path, token=None, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(API + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req) as resp:
            text = resp.read().decode()
            return resp.status, json.loads(text) if text else None
    except urllib.error.HTTPError as e:
        text = e.read().decode()
        try:
            return e.code, json.loads(text)
        except ValueError:
            return e.code, text


def check(label, status, expected, detail=""):
    ok = status == expected
    if not ok:
        failures.append(label)
    print(f"[{'PASS' if ok else 'FAIL'}] {label}: HTTP {status} (expected {expected}) {detail}")


def login(username):
    status, body = call("POST", "/auth/login", body={"username": username, "password": PASSWORD})
    check(f"login {username}", status, 200, f"-> role {body.get('role')}")
    return body["token"]


print(f"API workflow test - {date.today()}\n")
print("== 1. Authentication and role checks ==")
student = login("24BYB1001")
admin = login("admin@shms.local")
warden = login("warden@shms.local")
maint = login("maint@shms.local")
check("no token -> /hostels", call("GET", "/hostels")[0], 401)
check("student -> admin list of students", call("GET", "/students", student)[0], 403)
check("maintenance -> leave approvals", call("GET", "/leaves", maint)[0], 403)
check("student -> own profile", call("GET", "/students/me", student)[0], 200)

print("\n== 2. Room allocation (sp_allocate_room + occupancy trigger) ==")
_, waiting = call("GET", "/students/waiting", admin)
male = next(s for s in waiting if s["gender"] == "Male")
female = next(s for s in waiting if s["gender"] == "Female")
print(f"   waiting students: {len(waiting)}; using {male['registrationNo']} {male['name']}")
_, rooms = call("GET", "/rooms/available?gender=Male&hostelId=1", admin)
room = rooms[0]
print(f"   room {room['roomNumber']} before: occupied {room['occupiedCount']}/{room['capacity']}")
status, alloc = call("POST", "/allocations", admin, {"studentId": male["id"], "roomId": room["id"]})
check("allocate room", status, 201, f"-> allocation {alloc.get('id')}, bed {alloc.get('bedNumber')}")
_, room_after = call("GET", f"/rooms/{room['id']}", admin)
print(f"   room {room['roomNumber']} after : occupied {room_after['occupiedCount']}/{room_after['capacity']} (updated by trigger)")
status, err = call("POST", "/allocations", admin, {"studentId": male["id"], "roomId": room["id"]})
check("allocate same student again", status, 400, f"-> {err.get('message')}")
status, err = call("POST", "/allocations", admin, {"studentId": female["id"], "roomId": room["id"]})
check("allocate female into male hostel", status, 400, f"-> {err.get('message')}")
status, vac = call("PUT", f"/allocations/{alloc['id']}/vacate", admin)
check("vacate room", status, 200, f"-> status {vac.get('status')}, vacated {vac.get('vacateDate')}")
_, room_end = call("GET", f"/rooms/{room['id']}", admin)
print(f"   room {room['roomNumber']} after vacate: occupied {room_end['occupiedCount']}/{room_end['capacity']}")
status, alloc2 = call("POST", "/allocations", admin, {"studentId": male["id"], "roomId": room["id"]})
check("re-allocate (history row kept)", status, 201, f"-> allocation {alloc2.get('id')}")

print("\n== 3. Complaint lifecycle (sp_assign_complaint + status trigger) ==")
status, c = call("POST", "/complaints", student,
                 {"category": "Electrical", "description": "Study lamp socket is sparking when plugged in.", "priority": "High"})
check("student raises complaint", status, 201, f"-> id {c.get('id')}, status {c.get('status')}, room {c.get('roomNumber')}")
_, options = call("GET", f"/staff/maintenance-options?complaintId={c['id']}", admin)
print("   eligible staff (fn_staff_open_workload): " + ", ".join(f"{o['name']} [{o['workload']}]" for o in options))
_, plumbers = call("GET", "/staff?role=Maintenance&hostelId=1", admin)
plumber = next(s for s in plumbers if s["specialization"] == "Plumbing")
status, err = call("PUT", f"/complaints/{c['id']}/assign", admin, {"staffId": plumber["id"]})
check("assign to a plumber", status, 400, f"-> {err.get('message')}")
status, err = call("PUT", f"/complaints/{c['id']}/status", maint, {"status": "In Progress"})
check("maintenance updates unassigned complaint", status, 403)
status, c2 = call("PUT", f"/complaints/{c['id']}/assign", admin, {"staffId": 3, "scheduledDate": str(date.today())})
check("assign to electrician (maint@shms.local)", status, 200, f"-> status {c2.get('status')}, staff {c2.get('assignedToName')}")
status, c3 = call("PUT", f"/complaints/{c['id']}/status", maint, {"status": "In Progress", "remarks": "Inspecting wiring"})
check("maintenance starts work", status, 200, f"-> {c3.get('status')}")
status, c4 = call("PUT", f"/complaints/{c['id']}/status", maint, {"status": "Resolved", "remarks": "Socket replaced"})
check("maintenance resolves", status, 200, f"-> {c4.get('status')}, resolvedDate {c4.get('resolvedDate')} (set by trigger)")

print("\n== 4. Leave approval (sp_decide_leave) ==")
status, leave = call("POST", "/leaves", student, {"startDate": "2026-10-20", "endDate": "2026-10-23",
                                                   "reason": "Cousin's wedding", "destination": "Pune"})
check("student applies for leave", status, 201, f"-> id {leave.get('id')}, {leave.get('status')}, {leave.get('days')} days")
status, err = call("PUT", f"/leaves/{leave['id']}/decision", student, {"decision": "Approved"})
check("student tries to approve own leave", status, 403)
status, dec = call("PUT", f"/leaves/{leave['id']}/decision", warden, {"decision": "Approved"})
check("warden approves", status, 200, f"-> {dec.get('status')} by {dec.get('approvedBy')}")
status, err = call("PUT", f"/leaves/{leave['id']}/decision", warden, {"decision": "Rejected"})
check("decide again", status, 400, f"-> {err.get('message')}")

print("\n== 5. Visitor pass + QR (trg_visitor_pass_qr) ==")
status, v = call("POST", "/visitors", student, {"visitorName": "Rakesh Mehta", "visitorPhone": "9876500011",
                                                "relationToStudent": "Father", "purpose": "Weekend visit",
                                                "visitDate": str(date.today())})
check("student requests pass", status, 201, f"-> id {v.get('id')}, {v.get('status')}, qr {v.get('qrCode')}")
status, v2 = call("PUT", f"/visitors/{v['id']}/decision", warden, {"decision": "Approved"})
check("warden approves pass", status, 200, f"-> qr {v2.get('qrCode')} (generated by trigger)")
status, s1 = call("POST", "/visitors/scan", warden, {"qrCode": v2["qrCode"]})
check("gate scan 1 (entry)", status, 200, f"-> entry {s1.get('entryTime')}")
status, s2 = call("POST", "/visitors/scan", warden, {"qrCode": v2["qrCode"]})
check("gate scan 2 (exit)", status, 200, f"-> exit {s2.get('exitTime')}, {s2.get('status')}")
status, err = call("POST", "/visitors/scan", warden, {"qrCode": v2["qrCode"]})
check("gate scan 3 (used pass)", status, 400, f"-> {err.get('message')}")

print("\n== 6. Cursor procedures ==")
_, before = call("GET", "/complaints?status=Open", admin)
status, res = call("POST", "/complaints/auto-assign", admin)
_, after = call("GET", "/complaints?status=Open", admin)
check("auto-assign open complaints", status, 200, f"-> {res}; open {len(before)} -> {len(after)}")
status, lines = call("GET", "/reports/hostel-cursor-report", admin)
check("hostel cursor report (DBMS_OUTPUT)", status, 200, f"-> {len(lines)} lines")
for line in lines[:7]:
    print("   | " + line)

print("\n== 7. Validation and constraint errors ==")
status, err = call("POST", "/leaves", student, {"startDate": "2026-10-20", "endDate": "2026-10-10", "reason": "x"})
check("leave ending before it starts", status, 400, f"-> {err.get('message')}")
status, err = call("DELETE", f"/students/{male['id']}", admin)
check("delete student who has allocations (FK)", status, 409, f"-> {err.get('message')}")
status, err = call("POST", "/rooms", admin, {"hostelId": 1, "roomNumber": "AR-101", "floorNo": 1, "roomType": "Double"})
check("duplicate room number", status, 400, f"-> {err.get('message')}")

print(f"\nRESULT: {'ALL PASSED' if not failures else 'FAILED: ' + ', '.join(failures)}")
sys.exit(1 if failures else 0)
