"""
Generates database/seed/08_seed_data.sql for the Smart Hostel Management System.

DATA SOURCE: SYNTHETIC. All names, registration numbers, phone numbers and e-mails
are randomly generated from generic name lists. No real person's data is used.

The random generator is seeded, so running this script again produces exactly
the same SQL file. Reference "today" is fixed at 2026-10-07.

Run:  .venv/bin/python database/seed/generate_seed.py
"""
import random
from datetime import date, datetime, timedelta
from pathlib import Path

import bcrypt

random.seed(2026)
TODAY = date(2026, 10, 7)
OUT = Path(__file__).with_name("08_seed_data.sql")

# Every seeded application account uses this password (stored only as a BCrypt hash).
DEMO_PASSWORD = "Hostel@123"
PASSWORD_HASH = bcrypt.hashpw(DEMO_PASSWORD.encode(), bcrypt.gensalt(10)).decode()

MALE_FIRST = ["Aarav", "Vivaan", "Aditya", "Arjun", "Rohan", "Karan", "Ishaan", "Siddharth", "Kabir", "Rahul",
              "Aniket", "Yash", "Pranav", "Harsh", "Nikhil", "Varun", "Dev", "Manav", "Kunal", "Tanmay",
              "Rishi", "Abhinav", "Akash", "Gaurav", "Sahil", "Om", "Ayaan", "Reyansh", "Shaurya", "Atharv",
              "Vedant", "Parth", "Sarthak", "Mihir", "Naveen", "Suresh", "Prakash", "Rajesh", "Vikram", "Deepak"]
FEMALE_FIRST = ["Ananya", "Diya", "Saanvi", "Aadhya", "Ishita", "Kavya", "Meera", "Riya", "Sneha", "Pooja",
                "Nisha", "Priya", "Aditi", "Tanvi", "Shreya", "Neha", "Anjali", "Divya", "Simran", "Kritika",
                "Lavanya", "Sakshi", "Muskan", "Vaishnavi", "Harini", "Janani", "Keerthana", "Aishwarya", "Swati", "Nandini"]
LAST = ["Sharma", "Verma", "Gupta", "Singh", "Kumar", "Patel", "Reddy", "Nair", "Iyer", "Menon",
        "Joshi", "Kulkarni", "Deshmukh", "Chopra", "Malhotra", "Kapoor", "Bose", "Das", "Mukherjee", "Banerjee",
        "Rao", "Pillai", "Agarwal", "Bansal", "Mehta", "Shah", "Jain", "Saxena", "Tiwari", "Mishra",
        "Pandey", "Chauhan", "Yadav", "Rathore", "Sethi", "Bhatt", "Krishnan", "Subramanian", "Naidu", "Gill"]

BRANCHES = [("BCE", "Computer Science and Engineering"), ("BAI", "CSE (Artificial Intelligence)"),
            ("BIT", "Information Technology"), ("BEC", "Electronics and Communication"),
            ("BME", "Mechanical Engineering"), ("BYB", "CSE (Cyber Security)")]

HOSTELS = [  # id, name, gender, location, floors
    (1, "Aravali Block", "Male", "North Campus, near Main Gate", 5),
    (2, "Nilgiri Block", "Male", "North Campus, behind Library", 5),
    (3, "Shivalik Block", "Male", "East Campus, near Sports Complex", 5),
    (4, "Vindhya Block", "Male", "East Campus, near Food Court", 5),
    (5, "Ganga Block", "Female", "South Campus, near Health Centre", 5),
    (6, "Kaveri Block", "Female", "South Campus, near Auditorium", 5),
]
HOSTEL_CODE = {1: "AR", 2: "NI", 3: "SH", 4: "VI", 5: "GA", 6: "KA"}
ROOM_TYPES = [("Single", 1), ("Double", 2), ("Triple", 3), ("Quad", 4)]
SPECIALIZATIONS = ["Electrical", "Plumbing", "Carpentry", "Housekeeping", "Network"]

lines = []


def q(value):
    """Format a Python value as an Oracle SQL literal."""
    if value is None:
        return "NULL"
    if isinstance(value, datetime):
        return f"TO_TIMESTAMP('{value:%Y-%m-%d %H:%M:%S}', 'YYYY-MM-DD HH24:MI:SS')"
    if isinstance(value, date):
        return f"DATE '{value:%Y-%m-%d}'"
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace("'", "''") + "'"


def insert(table, columns, values):
    lines.append(f"INSERT INTO {table} ({', '.join(columns)}) VALUES ({', '.join(q(v) for v in values)});")


def section(title):
    lines.append("")
    lines.append(f"-- ---------------- {title} ----------------")


def rand_date(start, end):
    return start + timedelta(days=random.randint(0, (end - start).days))


def rand_time(d, h1=8, h2=21):
    return datetime(d.year, d.month, d.day, random.randint(h1, h2), random.randint(0, 59), random.randint(0, 59))


used_phones = set()


def phone():
    while True:
        p = random.choice("6789") + "".join(random.choice("0123456789") for _ in range(9))
        if p not in used_phones:
            used_phones.add(p)
            return p


used_names = {"Rajesh Kulkarni", "Suresh Iyer", "Prakash Naidu", "Meera Krishnan", "Aarav Mehta"}


def person(gender):
    first_list = MALE_FIRST if gender == "Male" else FEMALE_FIRST
    while True:
        name = f"{random.choice(first_list)} {random.choice(LAST)}"
        if name not in used_names:
            used_names.add(name)
            return name


# ------------------------------------------------------------------ HOSTEL
section("HOSTEL")
for h in HOSTELS:
    insert("hostel", ["hostel_id", "hostel_name", "gender_type", "location", "total_floors"], h)

# ------------------------------------------------------------------ ROOM
section("ROOM")
rooms = []  # dicts
room_id = 0
for hid, _, gender, _, floors in HOSTELS:
    for floor in range(1, floors + 1):
        for n in range(1, 7):  # 6 rooms per floor -> 30 rooms per hostel
            room_id += 1
            rtype, cap = random.choices(ROOM_TYPES, weights=[10, 40, 35, 15])[0]
            r = {"id": room_id, "hostel": hid, "gender": gender, "cap": cap, "type": rtype,
                 "number": f"{HOSTEL_CODE[hid]}-{floor}{n:02d}", "beds": set()}
            rooms.append(r)
            # occupied_count starts at 0; trg_allocation_occupancy fills it in as allocations are inserted
            insert("room", ["room_id", "room_number", "floor_no", "room_type", "capacity", "hostel_id"],
                   [room_id, r["number"], floor, rtype, cap, hid])

# ------------------------------------------------------------------ STAFF
section("STAFF  (all seeded accounts use password " + DEMO_PASSWORD + ")")
staff = []
staff_id = 0


def add_staff(role, hostel, spec=None, email=None, name=None, gender=None):
    global staff_id
    staff_id += 1
    gender = gender or random.choice(["Male", "Female"])
    name = name or person(gender)
    used_names.add(name)
    if email is None:
        email = name.lower().replace(" ", ".") + f"{staff_id}@shms.local"
    s = {"id": staff_id, "role": role, "hostel": hostel, "spec": spec, "name": name}
    staff.append(s)
    insert("staff", ["staff_id", "name", "role", "specialization", "phone", "email", "hostel_id", "password_hash"],
           [staff_id, name, role, spec, phone(), email, hostel, PASSWORD_HASH])
    return s


# demo accounts first (fixed e-mails documented in README)
add_staff("Admin", 1, email="admin@shms.local", name="Rajesh Kulkarni", gender="Male")
add_staff("Warden", 1, email="warden@shms.local", name="Suresh Iyer", gender="Male")
add_staff("Maintenance", 1, "Electrical", email="maint@shms.local", name="Prakash Naidu", gender="Male")
add_staff("Admin", 1, name="Meera Krishnan", gender="Female")
for hid, _, hgender, _, _ in HOSTELS:
    if hid != 1:
        add_staff("Warden", hid, gender=hgender)
    add_staff("Warden", hid, gender=hgender)  # deputy warden
    for spec in SPECIALIZATIONS:
        if not (hid == 1 and spec == "Electrical"):
            add_staff("Maintenance", hid, spec, gender="Male")
    for spec in random.sample(["Electrical", "Plumbing", "Housekeeping"], 1):
        add_staff("Maintenance", hid, spec, gender="Male")

wardens_by_hostel = {h[0]: [s for s in staff if s["role"] == "Warden" and s["hostel"] == h[0]] for h in HOSTELS}
maint_by_hostel_spec = {}
for s in staff:
    if s["role"] == "Maintenance":
        maint_by_hostel_spec.setdefault((s["hostel"], s["spec"]), []).append(s)
allocators = [s for s in staff if s["role"] in ("Admin", "Warden")]

# ------------------------------------------------------------------ STUDENT
section("STUDENT")
students = []
student_id = 0
year_prefix = {1: "26", 2: "25", 3: "24", 4: "23"}
reg_counter = {}
used_regs = {"24BYB1001"}  # reserved for the demo student account
for gender, count in (("Male", 245), ("Female", 115)):
    for _ in range(count):
        student_id += 1
        year = random.choices([1, 2, 3, 4], weights=[30, 28, 24, 18])[0]
        code, branch = random.choice(BRANCHES)
        key = (year, code)
        reg = None
        while reg is None or reg in used_regs:
            reg_counter[key] = reg_counter.get(key, 1000) + random.randint(1, 9)
            reg = f"{year_prefix[year]}{code}{reg_counter[key]:04d}"
        name = person(gender)
        # the first student is the documented demo student account
        if student_id == 1:
            reg, name, year, code, branch = "24BYB1001", "Aarav Mehta", 3, "BYB", "CSE (Cyber Security)"
        used_regs.add(reg)
        used_names.add(name)
        email = name.lower().replace(" ", ".") + f".{reg.lower()}@student.shms.local"
        cgpa = round(min(10, max(5.2, random.gauss(7.9, 0.9))), 2)
        st = {"id": student_id, "gender": gender, "year": year, "reg": reg, "name": name}
        students.append(st)
        insert("student", ["student_id", "registration_no", "name", "email", "phone", "gender", "year",
                           "branch", "cgpa", "password_hash"],
               [student_id, reg, name, email, phone(), gender, year, branch, cgpa, PASSWORD_HASH])

# ------------------------------------------------------------------ ROOM_ALLOCATION
section("ROOM_ALLOCATION  (history first, then current active allocations)")
alloc_id = 0
current_room = {}  # student_id -> room dict
sem_start = date(2026, 7, 15)


def free_bed(r):
    for b in range(1, r["cap"] + 1):
        if b not in r["beds"]:
            return b
    return None


# previous-semester allocations of senior students (Vacated) and a few Cancelled ones
for st in students:
    if st["year"] >= 2 and random.random() < 0.30:
        r = random.choice([r for r in rooms if r["gender"] == st["gender"]])
        alloc_id += 1
        start = rand_date(date(2025, 7, 10), date(2025, 7, 25))
        insert("room_allocation", ["allocation_id", "student_id", "room_id", "allocated_by", "bed_number",
                                   "allocation_date", "vacate_date", "status"],
               [alloc_id, st["id"], r["id"], random.choice(allocators)["id"], random.randint(1, r["cap"]),
                start, rand_date(date(2026, 5, 1), date(2026, 5, 20)), "Vacated"])
for st in random.sample(students, 6):
    r = random.choice([r for r in rooms if r["gender"] == st["gender"]])
    alloc_id += 1
    d = rand_date(date(2026, 7, 10), date(2026, 7, 14))
    insert("room_allocation", ["allocation_id", "student_id", "room_id", "allocated_by", "bed_number",
                               "allocation_date", "vacate_date", "status"],
           [alloc_id, st["id"], r["id"], random.choice(allocators)["id"], 1, d, d, "Cancelled"])

# current semester: ~92% of students have an active bed; rest are waiting for allocation
unallocated_ids = set()
for st in students:
    if st["id"] != 1 and random.random() < 0.08:
        unallocated_ids.add(st["id"])
        continue
    options = [r for r in rooms if r["gender"] == st["gender"] and free_bed(r)]
    if st["id"] == 1:
        options = [r for r in rooms if r["hostel"] == 1 and r["cap"] == 2]
    r = random.choice(options)
    bed = free_bed(r)
    r["beds"].add(bed)
    current_room[st["id"]] = r
    alloc_id += 1
    insert("room_allocation", ["allocation_id", "student_id", "room_id", "allocated_by", "bed_number",
                               "allocation_date", "status"],
           [alloc_id, st["id"], r["id"],
            random.choice([s for s in allocators if s["hostel"] in (1, r["hostel"])])["id"],
            bed, rand_date(sem_start, date(2026, 7, 31)), "Active"])

# ------------------------------------------------------------------ COMPLAINT
section("COMPLAINT")
COMPLAINT_TEXT = {
    "Electrical": ["Ceiling fan is not working and makes a grinding noise.", "Tube light flickers continuously.",
                   "Power socket near the study table is loose and sparks.", "Room has no power since morning; MCB trips repeatedly.",
                   "Geyser in the bathroom is not heating water.", "Switch board cover is broken and wires are exposed."],
    "Plumbing": ["Water leaking from the bathroom tap all night.", "Wash basin drain is blocked.",
                 "Flush tank is not working in the attached bathroom.", "Low water pressure in the shower.",
                 "Water seepage on the wall next to the bathroom.", "Pipe under the sink is leaking."],
    "Carpentry": ["Cupboard door hinge is broken.", "Study table drawer is jammed.", "Bed plank is cracked.",
                  "Room door does not lock properly.", "Window frame is loose and does not close.", "Chair leg is broken."],
    "Housekeeping": ["Corridor outside the room has not been cleaned for two days.", "Dustbin on the floor is overflowing.",
                     "Common washroom needs cleaning.", "Cobwebs and dust on the room ceiling.", "Pest problem (cockroaches) in the room.",
                     "Water cooler area is dirty."],
    "Network": ["Wi-Fi keeps disconnecting every few minutes.", "LAN port in the room is not working.",
                "Very slow internet speed on this floor at night.", "Unable to log in to the hostel Wi-Fi portal.",
                "Access point on the corridor appears to be switched off."],
    "Other": ["Room number plate is missing.", "Mosquito mesh on the window is torn.", "Notice board on the floor is damaged.",
              "Request for an additional bucket and mug.", "Drinking water purifier on the floor shows an error light."],
}
REMARKS = ["Issue fixed and tested.", "Replaced the faulty part.", "Repaired on site; student confirmed.",
           "Cleaned and sanitised the area.", "Router restarted and configuration updated.", "Work completed within the scheduled slot."]
RESOLVE_HOURS = {"Housekeeping": (2, 30), "Network": (4, 60), "Electrical": (3, 72),
                 "Plumbing": (6, 96), "Other": (12, 120), "Carpentry": (24, 168)}
complaint_id = 0
allocated_students = [s for s in students if s["id"] in current_room]
for i in range(460):
    st = students[0] if i < 6 else random.choice(allocated_students)
    r = current_room[st["id"]]
    cat = random.choices(list(COMPLAINT_TEXT), weights=[24, 20, 14, 18, 16, 8])[0]
    created_day = rand_date(sem_start + timedelta(days=5), TODAY)
    if i < 6:  # demo student's complaints: older ones resolved, newest ones still open
        created_day = TODAY - timedelta(days=[40, 25, 6, 3, 1, 0][i])
    created = rand_time(created_day, 8, 11)
    age = (TODAY - created_day).days
    # older complaints are mostly resolved; recent ones are still in the pipeline
    if age > 30:
        status = "Resolved"
    elif age > 20:
        status = random.choices(["Resolved", "In Progress"], weights=[90, 10])[0]
    elif age > 6:
        status = random.choices(["Resolved", "In Progress", "Assigned"], weights=[65, 20, 15])[0]
    else:
        status = random.choices(["Resolved", "In Progress", "Assigned", "Open"], weights=[15, 20, 25, 40])[0]
    if i < 6:  # demo student: one of each state
        status = ["Resolved", "Resolved", "In Progress", "Assigned", "Open", "Open"][i]
    assigned = None
    if status != "Open":
        spec = random.choice(SPECIALIZATIONS) if cat == "Other" else cat
        assigned = random.choice(maint_by_hostel_spec[(r["hostel"], spec)])["id"]
    priority = random.choices(["Low", "Medium", "High"], weights=[25, 50, 25])[0]
    resolved = None
    remarks = None
    if status == "Resolved":
        # typical turnaround depends on the kind of work; high priority is handled faster
        lo, hi = RESOLVE_HOURS[cat]
        hours = random.randint(lo, hi) * (0.5 if priority == "High" else 1)
        resolved = created + timedelta(hours=max(2, hours))
        if resolved.date() > TODAY:
            resolved = created + timedelta(minutes=45)
        remarks = random.choice(REMARKS)
    scheduled = None
    if status in ("Assigned", "In Progress"):
        scheduled = created_day + timedelta(days=random.randint(1, 3))
    complaint_id += 1
    insert("complaint", ["complaint_id", "student_id", "assigned_to", "category", "description", "priority", "status",
                         "created_date", "resolved_date", "remarks", "scheduled_date"],
           [complaint_id, st["id"], assigned, cat, random.choice(COMPLAINT_TEXT[cat]), priority, status,
            created, resolved, remarks, scheduled])

# ------------------------------------------------------------------ LEAVE_REQUEST
section("LEAVE_REQUEST")
LEAVE_REASONS = [("Going home for Diwali festival", None), ("Sister's wedding", None), ("Medical check-up at home town", None),
                 ("Attending a national-level hackathon", None), ("Family function", None), ("Internship interview", None),
                 ("Personal work at home", None), ("Visiting parents for the weekend", None), ("Inter-college sports meet", None),
                 ("Passport verification appointment", None)]
CITIES = ["Chennai", "Bengaluru", "Hyderabad", "Mumbai", "Pune", "New Delhi", "Kolkata", "Jaipur", "Lucknow", "Kochi",
          "Coimbatore", "Bhopal", "Chandigarh", "Patna", "Nagpur", "Indore", "Visakhapatnam", "Madurai"]
leave_id = 0
for i in range(270):
    st = students[0] if i < 4 else random.choice(allocated_students)
    applied = rand_date(sem_start + timedelta(days=3), TODAY)
    start = applied + timedelta(days=random.randint(1, 10))
    end = start + timedelta(days=random.randint(1, 6))
    if start > TODAY:
        status = random.choices(["Pending", "Approved", "Rejected"], weights=[55, 35, 10])[0]
    else:
        status = random.choices(["Approved", "Rejected"], weights=[85, 15])[0]
    if i < 4:
        status = ["Approved", "Rejected", "Pending", "Pending"][i]
        if status == "Pending":
            applied = TODAY - timedelta(days=1)
            start, end = TODAY + timedelta(days=3 + i), TODAY + timedelta(days=6 + i)
    hostel = current_room[st["id"]]["hostel"]
    approver = random.choice(wardens_by_hostel[hostel])["id"] if status != "Pending" else None
    leave_id += 1
    insert("leave_request", ["leave_id", "student_id", "approved_by", "start_date", "end_date", "reason",
                             "destination", "applied_date", "status"],
           [leave_id, st["id"], approver, start, end, random.choice(LEAVE_REASONS)[0], random.choice(CITIES),
            applied, status])

# ------------------------------------------------------------------ VISITOR_PASS
section("VISITOR_PASS  (qr_code is generated by trg_visitor_pass_qr)")
RELATIONS = ["Father", "Mother", "Brother", "Sister", "Uncle", "Aunt", "Guardian", "Cousin", "Friend"]
PURPOSES = ["Weekend visit", "Dropping off winter clothes", "Bringing medicines", "Parent-teacher meeting",
            "Collecting documents", "Birthday visit", "Helping with room shifting", "Visiting after exams"]
pass_id = 0
for i in range(270):
    st = students[0] if i < 4 else random.choice(allocated_students)
    visit = rand_date(sem_start + timedelta(days=7), TODAY + timedelta(days=12))
    if visit > TODAY:
        status = random.choices(["Pending", "Approved", "Rejected"], weights=[55, 35, 10])[0]
    elif visit == TODAY:
        status = random.choice(["Approved", "Pending"])
    else:
        status = random.choices(["Completed", "Rejected"], weights=[88, 12])[0]
    if i < 4:
        status = ["Completed", "Approved", "Pending", "Rejected"][i]
        visit = [TODAY - timedelta(days=9), TODAY + timedelta(days=2), TODAY + timedelta(days=5), TODAY - timedelta(days=3)][i]
    entry = exit_ = None
    if status == "Completed":
        entry = rand_time(visit, 9, 16)
        exit_ = entry + timedelta(minutes=random.randint(30, 240))
    relation = random.choice(RELATIONS)
    vgender = "Female" if relation in ("Mother", "Sister", "Aunt") else ("Male" if relation in ("Father", "Brother", "Uncle") else random.choice(["Male", "Female"]))
    surname = st["name"].split()[-1] if relation in ("Father", "Mother", "Brother", "Sister") else random.choice(LAST)
    vname = f"{random.choice(MALE_FIRST if vgender == 'Male' else FEMALE_FIRST)} {surname}"
    hostel = current_room[st["id"]]["hostel"]
    approver = random.choice(wardens_by_hostel[hostel])["id"] if status != "Pending" else None
    pass_id += 1
    insert("visitor_pass", ["pass_id", "student_id", "approved_by", "visitor_name", "visitor_phone",
                            "relation_to_student", "purpose", "visit_date", "entry_time", "exit_time", "status"],
           [pass_id, st["id"], approver, vname, phone(), relation, random.choice(PURPOSES), visit, entry, exit_, status])

# ================================================================== EXTENSION TABLES
# ------------------------------------------------------------------ FEE_PAYMENT
section("FEE_PAYMENT (extension)")
RENT = {"Single": 82000, "Double": 64000, "Triple": 52000, "Quad": 45000}
MESS_FEE = 27500
payment_id = 0
txn = 4400000000


def fee_row(st_id, semester, fee_type, due, due_date, paid_ratio):
    global payment_id, txn
    payment_id += 1
    paid = round(due * paid_ratio, 2)
    paid_date = mode = ref = None
    if paid > 0:
        paid_date = due_date - timedelta(days=random.randint(0, 25))
        mode = random.choices(["Online", "Card", "DD", "Cash"], weights=[60, 20, 12, 8])[0]
        txn += random.randint(1, 999)
        ref = f"TXN{txn}"
    insert("fee_payment", ["payment_id", "student_id", "semester", "fee_type", "amount_due", "amount_paid",
                           "due_date", "paid_date", "payment_mode", "transaction_ref"],
           [payment_id, st_id, semester, fee_type, due, paid, due_date, paid_date, mode, ref])


for st in students:
    if st["id"] not in current_room:
        continue
    rtype = current_room[st["id"]]["type"]
    if st["year"] == 1:
        fee_row(st["id"], "2026-27 Odd", "Caution Deposit", 10000, date(2026, 7, 20), 1)
    else:
        fee_row(st["id"], "2025-26 Even", "Hostel Rent", RENT[rtype], date(2026, 1, 10), 1)
        fee_row(st["id"], "2025-26 Even", "Mess", MESS_FEE, date(2026, 1, 10), 1)
    ratio = random.choices([1, 0.5, 0], weights=[78, 12, 10])[0]
    if st["id"] == 1:
        ratio = 1
    fee_row(st["id"], "2026-27 Odd", "Hostel Rent", RENT[rtype], date(2026, 7, 31), ratio)
    fee_row(st["id"], "2026-27 Odd", "Mess", MESS_FEE, date(2026, 7, 31), 1 if st["id"] == 1 else random.choices([1, 0], weights=[88, 12])[0])

# ------------------------------------------------------------------ ANNOUNCEMENT
section("ANNOUNCEMENT (extension)")
ANNOUNCEMENTS = [
    ("Water supply interruption on Saturday", "Water supply will be shut off between 10 AM and 2 PM on Saturday for overhead tank cleaning. Please store water in advance.", "Maintenance", None),
    ("Diwali leave applications", "Students going home for Diwali must apply for leave through the portal at least three days before departure.", "General", None),
    ("Mess feedback week", "The mess committee is collecting suggestions for the next month's menu. Share your feedback with your floor representative.", "Mess", None),
    ("Fire drill on Thursday", "A mandatory fire evacuation drill will be conducted on Thursday at 6 PM. All residents must assemble at the main lawn.", "Urgent", None),
    ("Wi-Fi maintenance window", "The hostel network will be upgraded on Sunday from 1 AM to 5 AM. Internet access may be intermittent.", "Maintenance", None),
    ("Hostel day celebrations", "Hostel day will be held next Friday evening with cultural events and a special dinner.", "Event", None),
    ("Room inspection schedule", "Wardens will carry out room inspections floor-wise during the coming week. Keep rooms clean and electrical appliances unplugged.", "General", None),
    ("Pest control drive", "Pest control treatment will be carried out floor-wise. Keep food items sealed and cupboards closed.", "Maintenance", None),
    ("Fee payment reminder", "Odd semester hostel and mess fees were due on 31 July. Students with pending dues must clear them at the earliest.", "Urgent", None),
    ("Yoga sessions every morning", "Free yoga sessions are held at the hostel terrace from 6:15 AM to 7:00 AM on weekdays.", "Event", None),
    ("Lost and found items on display", "Unclaimed items reported in the lost and found portal are kept at the warden office. Collect them with ID proof.", "General", None),
    ("Night curfew timings", "Hostel gates close at 10:30 PM. Late entry requires prior approval from the warden.", "General", None),
]
announcement_id = 0
for i, (title, body, cat, _) in enumerate(ANNOUNCEMENTS * 2):
    announcement_id += 1
    hostel = None if i < len(ANNOUNCEMENTS) else HOSTELS[i % 6][0]
    poster = staff[0] if hostel is None else random.choice(wardens_by_hostel[hostel])
    posted = rand_time(rand_date(date(2026, 8, 1), TODAY))
    insert("announcement", ["announcement_id", "title", "body", "category", "posted_by", "hostel_id", "posted_date",
                            "expiry_date", "is_pinned"],
           [announcement_id, title if hostel is None else f"{title} ({HOSTELS[hostel - 1][1]})", body, cat,
            poster["id"], hostel, posted, posted.date() + timedelta(days=30), "Y" if cat == "Urgent" else "N"])

# ------------------------------------------------------------------ MARKETPLACE_LISTING
section("MARKETPLACE_LISTING (extension)")
ITEMS = [("Engineering Mathematics textbook (Grewal)", "Books", 350), ("Data Structures in C book", "Books", 280),
         ("Casio fx-991ES scientific calculator", "Electronics", 700), ("Hero Sprint bicycle", "Cycle", 3500),
         ("Study table lamp (LED)", "Electronics", 450), ("Single bed mattress", "Bedding", 1200),
         ("Pillow and bedsheet set", "Bedding", 400), ("Plastic chair", "Furniture", 300),
         ("Electric kettle 1.5L", "Electronics", 550), ("Mini table fan", "Electronics", 650),
         ("Operating Systems (Galvin) book", "Books", 400), ("Drafting kit for engineering drawing", "Other", 250),
         ("Laptop cooling pad", "Electronics", 500), ("Bucket and mug set", "Other", 150), ("Shoe rack", "Furniture", 450),
         ("Firefox gear cycle", "Cycle", 5200), ("Extension board (4 sockets)", "Electronics", 300), ("Cricket bat", "Other", 900)]
listing_id = 0
for i in range(42):
    title, cat, base = ITEMS[i % len(ITEMS)]
    seller = random.choice(allocated_students)
    status = random.choices(["Available", "Sold", "Withdrawn"], weights=[60, 30, 10])[0]
    buyer = None
    if status == "Sold":
        buyer = random.choice([s for s in allocated_students if s["id"] != seller["id"]])["id"]
    listing_id += 1
    insert("marketplace_listing", ["listing_id", "seller_id", "buyer_id", "title", "description", "category", "price",
                                   "item_condition", "status", "listed_date"],
           [listing_id, seller["id"], buyer, title, f"{title} in usable condition. Contact me in my room for details.",
            cat, round(base * random.uniform(0.7, 1.2) / 10) * 10, random.choice(["New", "Like New", "Good", "Fair"]),
            status, rand_date(date(2026, 7, 20), TODAY)])

# ------------------------------------------------------------------ INVENTORY_ITEM
section("INVENTORY_ITEM (extension)")
INVENTORY = [("Bed frame", "Furniture", 1), ("Mattress", "Bedding", 1), ("Study table", "Furniture", 1),
             ("Study chair", "Furniture", 1), ("Cupboard", "Furniture", 1), ("Ceiling fan", "Electrical", None),
             ("Tube light", "Electrical", None), ("Water cooler", "Appliance", None), ("Washing machine", "Appliance", None),
             ("Fire extinguisher", "Other", None), ("Vacuum cleaner", "Cleaning", None)]
item_id = 0
for hid, *_ in HOSTELS:
    beds = sum(r["cap"] for r in rooms if r["hostel"] == hid)
    nrooms = sum(1 for r in rooms if r["hostel"] == hid)
    fixed = {"Ceiling fan": nrooms + 10, "Tube light": nrooms * 2 + 20, "Water cooler": 5, "Washing machine": 4,
             "Fire extinguisher": 15, "Vacuum cleaner": 3}
    for name, cat, per_bed in INVENTORY:
        total = beds + random.randint(2, 8) if per_bed else fixed[name]
        item_id += 1
        insert("inventory_item", ["item_id", "hostel_id", "item_name", "category", "quantity_total",
                                  "quantity_damaged", "last_checked"],
               [item_id, hid, name, cat, total, random.randint(0, max(1, total // 12)),
                rand_date(date(2026, 8, 15), TODAY)])

# ------------------------------------------------------------------ LOST_FOUND_ITEM
section("LOST_FOUND_ITEM (extension)")
LF_ITEMS = ["Black wallet", "Blue water bottle", "Student ID card", "Boat earphones", "Room key with red tag",
            "Grey hoodie", "Scientific calculator", "Spectacles in brown case", "Umbrella", "Laptop charger",
            "Silver wrist watch", "Notebook (Physics)", "Cycle lock key", "Black backpack", "Pen drive 32GB"]
LF_PLACES = ["Mess hall", "Reading room", "Gym", "Laundry area", "Ground floor corridor", "Terrace", "TV room",
             "Main gate", "Badminton court", "Common washroom"]
lf_id = 0
for i in range(32):
    reporter = random.choice(allocated_students)
    rtype = random.choice(["Lost", "Found"])
    status = random.choices(["Open", "Claimed", "Closed"], weights=[50, 35, 15])[0]
    claimed = random.choice([s for s in allocated_students if s["id"] != reporter["id"]])["id"] if status == "Claimed" else None
    lf_id += 1
    item = random.choice(LF_ITEMS)
    insert("lost_found_item", ["item_id", "reported_by", "claimed_by", "report_type", "item_name", "description",
                               "location", "report_date", "status"],
           [lf_id, reporter["id"], claimed, rtype, item,
            f"{item} {'lost' if rtype == 'Lost' else 'found'} near the {random.choice(LF_PLACES).lower()}.",
            random.choice(LF_PLACES), rand_date(date(2026, 8, 1), TODAY), status])

# ------------------------------------------------------------------ MESS_MENU
section("MESS_MENU (extension)")
BREAKFAST = ["Idli, sambar, coconut chutney, tea/coffee", "Poha, boiled eggs, banana, tea/coffee",
             "Aloo paratha, curd, pickle, tea/coffee", "Dosa, sambar, chutney, milk", "Upma, vada, chutney, tea/coffee",
             "Bread, butter, jam, omelette, milk", "Puri, aloo sabzi, tea/coffee"]
LUNCH = ["Rice, dal tadka, aloo gobi, chapati, curd", "Jeera rice, rajma, mixed veg, chapati, salad",
         "Rice, sambar, rasam, cabbage poriyal, papad", "Veg biryani, raita, chicken curry/paneer curry",
         "Rice, dal makhani, bhindi fry, chapati, buttermilk", "Rice, chole, aloo matar, puri, salad",
         "Lemon rice, curd rice, potato fry, pickle"]
SNACKS = ["Samosa and tea", "Bread pakoda and coffee", "Biscuits and tea", "Veg puff and juice",
          "Bhel puri and tea", "Sundal and coffee", "Pav bhaji and tea"]
DINNER = ["Chapati, paneer butter masala, dal, rice", "Fried rice, gobi manchurian, soup",
          "Chapati, egg curry/kadai veg, rice, dal", "Rice, sambar, keerai, appalam, curd",
          "Chapati, chicken curry/mushroom masala, rice", "Pulao, dal fry, salad, gulab jamun",
          "Chapati, mixed veg, dal, rice, ice cream"]
DAYS = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"]
menu_id = 0
for hid, *_ in HOSTELS:
    shift = hid % 7
    for d, day in enumerate(DAYS):
        for meal, options in (("Breakfast", BREAKFAST), ("Lunch", LUNCH), ("Snacks", SNACKS), ("Dinner", DINNER)):
            menu_id += 1
            insert("mess_menu", ["menu_id", "hostel_id", "day_of_week", "meal_type", "menu_items"],
                   [menu_id, hid, day, meal, options[(d + shift) % 7]])

# ------------------------------------------------------------------ write file
identity_tables = [("hostel", "hostel_id"), ("room", "room_id"), ("staff", "staff_id"), ("student", "student_id"),
                   ("room_allocation", "allocation_id"), ("complaint", "complaint_id"), ("leave_request", "leave_id"),
                   ("visitor_pass", "pass_id"), ("fee_payment", "payment_id"), ("announcement", "announcement_id"),
                   ("marketplace_listing", "listing_id"), ("inventory_item", "item_id"),
                   ("lost_found_item", "item_id"), ("mess_menu", "menu_id")]

header = [
    "-- =====================================================================",
    "-- 08_seed_data.sql : SYNTHETIC dataset (generated by generate_seed.py - do not edit by hand)",
    f"-- All application accounts use the password '{DEMO_PASSWORD}' (stored as a BCrypt hash).",
    "-- Room occupied_count is NOT inserted: trg_allocation_occupancy computes it while",
    "-- the ROOM_ALLOCATION rows below are inserted.",
    "-- =====================================================================",
    "SET FEEDBACK OFF",
]
footer = ["", "COMMIT;", "", "-- Move each identity sequence past the explicitly inserted ids"]
footer += [f"ALTER TABLE {t} MODIFY ({c} GENERATED BY DEFAULT AS IDENTITY (START WITH LIMIT VALUE));"
           for t, c in identity_tables]
# The ALTER TABLE statements above are DDL, which marks dependent PL/SQL objects INVALID.
# Recompile them so every procedure, function and trigger is VALID again.
footer += ["", "-- ALTER TABLE (DDL) invalidates dependent PL/SQL; recompile it",
           "EXEC DBMS_UTILITY.COMPILE_SCHEMA(schema => USER, compile_all => FALSE);", "SET FEEDBACK ON"]
OUT.write_text("\n".join(header + lines + footer) + "\n")

print(f"Wrote {OUT}")
print(f"hostels={len(HOSTELS)} rooms={len(rooms)} staff={len(staff)} students={len(students)} "
      f"allocations={alloc_id} (active={len(current_room)}, waiting={len(unallocated_ids)}) complaints={complaint_id} "
      f"leaves={leave_id} visitors={pass_id} fees={payment_id} announcements={announcement_id} "
      f"listings={listing_id} inventory={item_id} lost_found={lf_id} menu={menu_id}")
