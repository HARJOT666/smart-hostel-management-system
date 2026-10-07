# Viva Notes — Smart Hostel Management System

Simple answers to the questions most likely to come up in the viva.
Everything here describes what is actually in the project.

---

## 1. The big picture

```
React (browser)
   │  HTTP + JSON, JWT in the "Authorization" header
   ▼
Spring Boot REST API
   ├── JWT authentication (Spring Security)
   ├── Redis cache        (a few slow-changing lists)
   └── Redis rate limit   (failed logins)
   │  JDBC
   ▼
Oracle Database 23ai  ← the PRIMARY database: every real record lives here
   tables + constraints + procedures + functions + cursor + triggers
```

**Why Oracle?** The DBMS lab needs SQL **and PL/SQL** (procedures, functions, cursors, triggers).
Oracle is the database PL/SQL was made for, and it is what the lab uses.

**Why Spring Boot?** It is the standard Java framework for REST APIs. It gives us Spring Data JPA for
simple database access, JdbcTemplate for calling PL/SQL, Spring Security for login, and Spring Cache for Redis,
all with very little setup.

**Why React?** It lets us build separate screens for students, wardens and maintenance staff
as one single-page web app that talks to the API.

**Why Docker?** Oracle and Redis run in containers, so anyone can start the exact same database with one
command (`docker compose up -d`) without installing Oracle on their laptop.

---

## 2. Database basics

| Term | Simple meaning | Example in our project |
|---|---|---|
| Primary key | A column that uniquely identifies each row | `STUDENT.Student_ID` |
| Foreign key | A column that must match a primary key in another table | `ROOM.Hostel_ID → HOSTEL.Hostel_ID` |
| NOT NULL | Value is compulsory | `STUDENT.Name` |
| UNIQUE | No two rows may have the same value | `STUDENT.Registration_No`, `STAFF.Email` |
| CHECK | Value must satisfy a condition | `CGPA BETWEEN 0 AND 10`, `Occupied_Count BETWEEN 0 AND Capacity` |
| DEFAULT | Value used when none is given | `COMPLAINT.Status DEFAULT 'Open'` |

**What do foreign keys protect?** You cannot allocate a room that does not exist (`ORA-02291`), and you cannot
delete a student who still has allocations or complaints (`ORA-02292`). We demonstrate both.

### Why each core table exists (Review 1 schema)

| Table | Why |
|---|---|
| HOSTEL | Each hostel block and whether it is for male or female students |
| ROOM | Rooms inside a hostel, with capacity and current occupancy |
| STUDENT | The residents (also their login, via `Password_Hash`) |
| STAFF | Admins, wardens and maintenance staff in one table with a `Role` column, because they share the same attributes |
| ROOM_ALLOCATION | Which student has which bed in which room. Student–Room is many-to-many over time, so this associative table resolves it and keeps history |
| COMPLAINT | Maintenance complaints and who is fixing them |
| LEAVE_REQUEST | Leave applications and the warden's decision |
| VISITOR_PASS | Visitor requests, approval, QR code, entry/exit times |

The six extra tables (FEE_PAYMENT, ANNOUNCEMENT, MARKETPLACE_LISTING, INVENTORY_ITEM, LOST_FOUND_ITEM,
MESS_MENU) are **application extensions** for the Software Engineering features. They only point to core tables
with foreign keys; the core design from Review 1 is unchanged.

### Two special rules
- **One active room per student** and **one student per bed**: enforced by *function-based unique indexes*
  (`CASE WHEN status = 'Active' THEN student_id END`). Old "Vacated" rows give NULL, so history is still allowed.
- **Fee status is not stored**: it is calculated from `amount_paid` vs `amount_due`. Storing it would create a
  transitive dependency (status depends on the amounts, not on the key) and break 3NF.

---

## 3. PL/SQL — why each piece exists

| Object | Type | What it does | Used by the app when… |
|---|---|---|---|
| `fn_available_beds` | function | capacity − occupied for a room | listing rooms with free beds |
| `fn_hostel_occupancy_pct` | function | % of beds occupied in a hostel | admin dashboard |
| `fn_staff_open_workload` | function | number of open jobs of a maintenance worker | choosing who to assign a complaint to |
| `fn_find_free_bed` | function | lowest free bed number in a room | inside `sp_allocate_room` |
| `sp_allocate_room` | procedure | checks everything and creates the allocation | admin clicks **Allocate** |
| `sp_vacate_room` | procedure | marks an allocation Vacated | admin clicks **Vacate** |
| `sp_assign_complaint` | procedure | checks role, specialization and hostel, then assigns | warden assigns a complaint |
| `sp_decide_leave` | procedure | Pending → Approved/Rejected by a warden only | warden approves/rejects leave |
| `sp_auto_assign_open_complaints` | **cursor** procedure | loops over every open complaint and assigns the least-busy matching worker | admin clicks **Auto-assign** |
| `sp_hostel_report` | **cursor** procedure | prints an occupancy report hostel by hostel | Reports page |
| `trg_allocation_validate` | trigger | bed number must exist in the room | every new allocation |
| `trg_allocation_occupancy` | trigger | keeps `ROOM.Occupied_Count` correct automatically | allocate / vacate |
| `trg_complaint_status` | trigger | Open→Assigned when staff is set; stamps `resolved_date`; blocks "Resolved" without staff | complaint updates |
| `trg_visitor_pass_qr` | trigger | creates a unique QR code value when a pass is approved | warden approves a visitor |

**What is a cursor?** A pointer that lets PL/SQL go through the rows of a query *one row at a time*
(OPEN → FETCH in a loop → CLOSE). We need it when each row needs its own decision, e.g. every open complaint
needs its own best staff member.

**What is a trigger?** Code that Oracle runs *automatically* when a row is inserted/updated/deleted.
Nobody can forget to update the occupancy, because the database does it itself.

### What happens during room allocation
1. Admin picks a student and a room in React → `POST /api/allocations`.
2. `AllocationService.allocate()` (one `@Transactional` method) calls `PlsqlRepository.allocateRoom()`.
3. Java runs `{call sp_allocate_room(?, ?, ?, ?, ?)}` through JDBC.
4. The procedure checks: student exists → no active allocation → staff is Admin/Warden → room exists
   (and **locks** the room row with `FOR UPDATE`) → gender matches → `fn_available_beds > 0` → picks a bed
   with `fn_find_free_bed` → INSERTs the allocation.
5. `trg_allocation_validate` checks the bed number; `trg_allocation_occupancy` adds 1 to `ROOM.Occupied_Count`.
6. The procedure returns the new allocation id and bed number (OUT parameters).
7. Spring commits the transaction, and React shows "Allocated bed 2 in AR-202".
8. If **any** check fails, Oracle raises e.g. `ORA-20013: Room is in a Male hostel; student is Female`;
   the whole transaction rolls back and the user sees that message (HTTP 400).

### How Spring Boot talks to Oracle
- **Spring Data JPA** (`StudentRepository`, …) for normal reads/writes. Hibernate is set to
  `ddl-auto: validate` — it checks the entities match the tables but never changes the schema.
- **JdbcTemplate** (`PlsqlRepository`) to call procedures/functions, and (`ReportRepository`) for report SQL.
- Both use the **same connection and transaction**, so a service method commits or rolls back everything together.
- Columns filled by triggers (`occupied_count`, `qr_code`, `resolved_date`) are marked `@Generated` in the entity:
  Hibernate never writes them and reads the trigger's value back after saving.

---

## 4. Login and JWT

**What is a JWT?** A signed text token with three parts: header . payload . signature.
Our payload holds the username, user id, role, name and expiry time (8 hours). The signature is made with a
secret key that only the server knows (HMAC-SHA256), so nobody can change the role inside the token.

**Login flow**
1. React sends username + password to `POST /api/auth/login`.
2. Redis check: is this IP blocked for too many failures? (see §6)
3. `CustomUserDetailsService` loads the user from Oracle: an e-mail → STAFF table, otherwise → STUDENT table
   (registration number).
4. Spring Security compares the password with the **BCrypt hash** stored in `Password_Hash`.
   Plain passwords are never stored.
5. If correct, `JwtService` creates the token and returns it.

**Every later request**
1. React sends `Authorization: Bearer <token>`.
2. `JwtAuthenticationFilter` checks the signature and expiry. If valid, the user is "logged in" for this request.
3. `@PreAuthorize("hasAnyRole('ADMIN','WARDEN')")` on the controller method decides if the role is allowed
   (otherwise 403). No token or a bad token → 401.
4. A student can only see their own data: the services use the user id **from the token**, never an id sent by the browser.

**Why stateless?** The server keeps no session; the token carries everything. That is simpler and works the same
if we ever run two copies of the backend.

---

## 5. Redis

### What is Redis?
Redis is an **in-memory key–value store**: you save a value under a key (like a giant `HashMap` running in its own
server) and can give each key an **expiry time**. Because it is in memory, reads take well under a millisecond.

### Why Oracle stays the primary database
Redis in our setup keeps nothing that cannot be rebuilt: only copies of data (the cache) and temporary counters.
Persistence is even switched off in `docker-compose.yml`. If Redis is wiped, nothing is lost — the next request
simply reads Oracle again. All real records, constraints, procedures and triggers are in Oracle.
If Redis is **down**, the app still works: cache errors are logged and the method reads Oracle
(`RedisCacheConfig.errorHandler`), and the login limiter lets logins through ("fails open").

### Why use Redis for caching?
Some lists are requested on almost every page but change rarely (hostels, mess menu, notices, contacts).
Reading them from Oracle each time repeats the same query. With the cache, the first request reads Oracle and saves
the result in Redis; the following requests get it straight from Redis.

### Which data is cached, and why

| Cache (Redis key) | TTL | Why it is safe to cache | Cleared when |
|---|---|---|---|
| `shms:hostels::all` | 60 min | Hostel names/locations almost never change | hostel created/updated |
| `shms:announcements::<hostel>` | 10 min | Notices change a few times a day | any notice created/edited/deleted |
| `shms:messMenu::<hostel>` | 6 h | Weekly menu | a meal is edited |
| `shms:emergencyContacts::<hostel>` | 60 min | Staff list changes rarely | staff created/edited/deleted |
| `shms:dashboardStats::admin` | 60 s | Heavy aggregate queries; being up to a minute old is acceptable for a summary | room allocated or vacated |

**Not cached on purpose:** room allocations, room occupancy, complaint status, leave status, visitor status,
fee/payment status. These change often and the user must see the exact current value.

**TTL (time-to-live):** how long Redis keeps a key before deleting it by itself. Even if we forgot an eviction,
stale data could live at most that long.

### How it is written in code
```java
@Cacheable(value = "hostels", key = "'all'")          // read: use Redis if present, else run method + store
public List<HostelResponse> getAllHostels() { ... }

@CacheEvict(value = "hostels", allEntries = true)     // write: remove the cached copy
public HostelResponse createHostel(HostelRequest r) { ... }
```

### What is cache invalidation?
Making sure the cache never keeps serving old data after the real data changes. We **evict** (delete) the cached key
whenever the matching data is written, so the next read goes to Oracle and caches the fresh result.
Tested: GET (Oracle) → GET (Redis) → POST notice (key deleted) → GET (Oracle again, includes the new notice).

### What is rate limiting? Why on login?
Limiting how many times someone can do something in a time window. Login is the endpoint attackers target to guess
passwords (brute force). Allowing only **5 failed attempts per IP address in 5 minutes** makes guessing impractical
but does not bother normal users.

### How Redis stores the counter
- Key: `rate:login:fail:<IP address>` (e.g. `rate:login:fail:127.0.0.1`)
- Value: number of failed attempts
- On a failure: `INCR` the key (Redis creates it at 1 if missing); when it becomes 1, `EXPIRE` it after 300 s.
- On a successful login the key is deleted.
- The key disappears by itself when it expires, so there is nothing to clean up.

### What happens when the limit is exceeded
Before checking the password, `LoginRateLimiter.checkAllowed()` reads the counter. If it is already 5, the request is
refused with **HTTP 429 Too Many Requests** and a `Retry-After` header (seconds until the key expires) —
even if the password is correct. After the key expires, logging in works again.
Tested: 5 × 401 → counter 1..5 → 6th attempt 429 → wait for expiry → 200.

*(Limitation to mention honestly: it counts per IP address, so many students behind one NAT share a counter.)*

---

## 6. Problems we hit (from docs/development-log.md)

1. **Duplicate registration number** in the generated data → caught by the UNIQUE constraint; fixed the generator.
2. **SQL*Plus committed half a failed script** (`EXIT` commits by default) → changed to `EXIT ... ROLLBACK`.
3. **PL/SQL became INVALID** after `ALTER TABLE` in the seed (DDL invalidates dependants) → recompile at the end.
4. **Unrealistic synthetic data** found through query output (same resolution time for all categories, wrong names)
   → improved the generator.
5. **Two demo tests checked the wrong rule** → fixed the test data.
6. **Time-zone bug:** Oracle container in UTC, app in IST → resolved time looked earlier than created time →
   `ORA-02290 CK_COMPLAINT_RESOLVED`. Fixed by running the Oracle container in `Asia/Kolkata`.
7. **Test-script bug** (`grep -c` exit code) → fixed.
