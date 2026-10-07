# Smart Hostel Management System

A web application that digitises hostel operations: room allocation, maintenance complaints, leave,
visitor passes, fees, notices, mess menu, marketplace, lost & found and inventory — for **students**,
**wardens/administrators** and **maintenance staff**.

One integrated project for two VIT courses:

| Course | Team |
|---|---|
| BCSE302L — Database Systems | Harjot Singh Bagga (24BYB0115), Tanuj Sharma (24BYB0122), Daksh Khanna (24BYB0125) |
| BCSE31P — Software Engineering | Harjot Singh Bagga (24BYB0115), Daksh Khanna (24BYB0125) |

School of Computer Science Engineering and Information Systems · B.Tech (Cybersecurity) · Vellore Institute of Technology

---

## 1. Architecture

```
React (Vite)  ──HTTP/JSON + JWT──►  Spring Boot 3 REST API  ──JDBC──►  Oracle Database 23ai Free
 localhost:5173                       localhost:8080                     localhost:1521 / FREEPDB1
                                         │                               tables, constraints,
                                         ├── JWT authentication          procedures, functions,
                                         ├── Redis cache          ──►   cursors, triggers
                                         └── Redis rate limiting  ──►  Redis 7 (localhost:6379)
```

- **Oracle is the primary database.** All records, rules (constraints) and business logic (PL/SQL) live there.
- **Redis** holds only rebuildable data: cached copies of slow-changing lists and short-lived login counters.
- Oracle and Redis run in Docker; the backend and frontend run on your machine (IntelliJ / npm).

More detail: [docs/architecture.md](docs/architecture.md).

## 2. Technologies

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite, React Router, Axios, Recharts, qrcode.react |
| Backend | Java 17, Spring Boot 3.5, Spring Web, Spring Data JPA (Hibernate), Spring Security, JJWT, Spring Cache, Spring Data Redis, Lombok, Maven |
| Database | Oracle Database 23ai Free (`gvenzl/oracle-free:23-slim`), SQL + PL/SQL |
| Cache / rate limit | Redis 7.4 |
| Tooling | Docker Desktop, IntelliJ IDEA, Git/GitHub, Python (data generator, tests, report builder) |

## 3. Project structure

```
smart-hostel-management/
├── backend/                        Spring Boot API (open this folder in IntelliJ)
│   └── src/main/java/com/smarthostel/
│       ├── config/                 Redis cache config, CORS + uploads, cache names
│       ├── controller/             REST controllers (one per module)
│       ├── service/                business logic (one per module)
│       ├── repository/             Spring Data repositories + PlsqlRepository + ReportRepository
│       ├── model/                  JPA entities (14 tables)
│       ├── dto/                    request/response records
│       ├── security/               JWT, filter, SecurityConfig, user details, login rate limiter
│       ├── exception/              global error handling
│       └── util/                   Oracle error message helper
├── frontend/                       React app
│   └── src/{api,auth,components,hooks,pages/{student,admin,maintenance,shared},utils}
├── database/
│   ├── docker-compose.yml          Oracle + Redis
│   ├── run-all.sh / reset-db.sh    create / rebuild the database
│   ├── schema/                     01 core tables, 02 core constraints, 03 extension tables
│   ├── functions/ procedures/ cursor/ triggers/   04–07 PL/SQL
│   ├── seed/                       08 seed data + generate_seed.py
│   ├── queries/                    09 business queries
│   └── transactions/               10 demo transactions
├── tools/                          test scripts, screenshot capture, report builder
└── docs/                           reports, evidence, development log, viva notes, design docs
```

## 4. Start the database (Docker)

Prerequisite: **Docker Desktop** running. (Settings → General → "Start Docker Desktop when you sign in"
makes the containers come back automatically after a reboot; both use `restart: unless-stopped`.)

```bash
cd database
cp .env.example .env            # first time only (holds the credentials, not committed to git)
docker compose up -d            # starts Oracle (shms-oracle) and Redis (shms-redis)
docker compose ps               # wait until both show "(healthy)" — Oracle takes ~40 s, first ever start ~2 min
```

First time only — create tables, PL/SQL and data:

```bash
./run-all.sh                    # runs scripts 01–08 in order, then prints row counts and object status
```

Rebuild everything from scratch at any time (deletes all app data): `./reset-db.sh`

Data is stored in the Docker volume `shms-oracle-data`, so it survives `docker compose down` / restarts.

### Oracle credentials (database login — NOT the app login)

| Setting | Value |
|---|---|
| Host / port | `localhost:1521` |
| Service name | `FREEPDB1` |
| Username | `user1` |
| Password | `user1Password` |
| JDBC URL | `jdbc:oracle:thin:@localhost:1521/FREEPDB1` |

SQL*Plus inside the container: `docker exec -it shms-oracle sqlplus user1/user1Password@localhost/FREEPDB1`
(or connect IntelliJ's Database tool / SQL Developer with the values above).

The Oracle container runs in time zone `Asia/Kolkata`, the same as the backend (see development log #6).

## 5. Run the backend in IntelliJ

1. **File → Open** → select the `backend` folder → open as a Maven project. Use **JDK 17**.
2. If asked, enable **annotation processing** (needed for Lombok).
3. Run `com.smarthostel.SmartHostelApplication`.
4. Check: <http://localhost:8080/actuator/health> → `"status":"UP"` with `db` (Oracle) and `redis` both UP.

All settings are in `backend/src/main/resources/application.yml` and can be overridden with environment
variables (Run Configuration → Environment variables): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_HOST`,
`REDIS_PORT`, `JWT_SECRET`, `FRONTEND_URL`, `UPLOAD_DIR`. No credentials are written in Java code.

Command line alternative: `cd backend && mvn spring-boot:run`

## 6. Run the frontend

```bash
cd frontend
npm install          # first time only
npm run dev
```

Open **<http://localhost:5173>** (use `localhost`, not `127.0.0.1` — the backend's CORS setting allows
`http://localhost:5173`). To use a different backend URL, copy `.env.example` to `.env` and set `VITE_API_URL`.

## 7. Demo login accounts (application logins)

Every seeded account has the password **`Hostel@123`** (stored only as a BCrypt hash).

| Role | Username |
|---|---|
| Student | `24BYB1001` (registration number) |
| Admin | `admin@shms.local` |
| Warden | `warden@shms.local` |
| Maintenance (electrician, Aravali Block) | `maint@shms.local` |

Any other seeded student logs in with their registration number; staff log in with their e-mail.
New students can register from the login page.

## 8. Dataset

**Synthetic data**, generated by `database/seed/generate_seed.py` (fixed random seed → identical every run).
No public dataset of hostel operations exists because such data is private student information, so names,
registration numbers, phones and e-mails are generated from generic name lists; e-mails use the fictional
domain `shms.local`. Seed sizes: 6 hostels, 180 rooms (468 beds), 50 staff, 360 students, 418 allocations,
460 complaints, 270 leave requests, 270 visitor passes, 1,232 fee rows, 24 notices, 42 listings,
66 inventory items, 32 lost & found reports, 168 menu entries.

## 9. Database scripts and PL/SQL

| Script | Contents |
|---|---|
| `schema/01_core_schema.sql` | 8 core tables from Review 1 (PK, NOT NULL, DEFAULT) |
| `schema/02_core_constraints.sql` | FK, UNIQUE, CHECK, function-based unique indexes, FK indexes |
| `schema/03_extension_tables.sql` | 6 application extension tables (SE modules) |
| `functions/04_functions.sql` | `fn_available_beds`, `fn_hostel_occupancy_pct`, `fn_staff_open_workload`, `fn_find_free_bed` |
| `procedures/05_procedures.sql` | `sp_allocate_room`, `sp_vacate_room`, `sp_assign_complaint`, `sp_decide_leave` |
| `cursor/06_cursor.sql` | `sp_auto_assign_open_complaints`, `sp_hostel_report` (explicit + parameterised cursors) |
| `triggers/07_triggers.sql` | `trg_allocation_validate`, `trg_allocation_occupancy`, `trg_complaint_status`, `trg_visitor_pass_qr` |
| `seed/08_seed_data.sql` | generated data |
| `queries/09_queries.sql` | 18 report queries + INSERT/UPDATE/DELETE (rolled back) |
| `transactions/10_demo_transactions.sql` | 9 transaction demos incl. rule violations (rolled back) |

Run a single script: `./database/run-sql.sh queries/09_queries.sql`

Every PL/SQL object is used by the application — see [docs/viva-notes.md](docs/viva-notes.md) §3.

## 10. Redis

| Cache | TTL | Evicted on |
|---|---|---|
| hostels | 60 min | hostel create/update |
| announcements (per hostel) | 10 min | notice create/update/delete |
| mess menu (per hostel) | 6 h | meal edit |
| emergency contacts (per hostel) | 60 min | staff create/update/delete |
| admin dashboard statistics | 60 s | room allocate/vacate |

Not cached on purpose: allocations, occupancy, complaint/leave/visitor status, fees.

Login rate limit: 5 failed attempts per IP within 5 minutes → HTTP 429 with `Retry-After`.
Redis key `rate:login:fail:<ip>` expires automatically.

Inspect Redis: `docker exec -it shms-redis redis-cli` then `KEYS shms:*`, `TTL shms:hostels::all`.

## 11. API overview

All endpoints are under `/api`; everything except login/register needs `Authorization: Bearer <token>`.
Full list: [docs/api.md](docs/api.md).

| Area | Examples |
|---|---|
| Auth | `POST /auth/login`, `POST /auth/register`, `GET /auth/me` |
| Student self-service | `GET /students/me`, `GET /complaints/my`, `GET /leaves/my`, `GET /visitors/my`, `GET /fees/my` |
| Allocation | `GET /students/waiting`, `GET /rooms/available?gender=`, `POST /allocations`, `PUT /allocations/{id}/vacate` |
| Complaints | `POST /complaints`, `PUT /complaints/{id}/assign`, `POST /complaints/auto-assign`, `PUT /complaints/{id}/status` |
| Leave / visitors | `PUT /leaves/{id}/decision`, `PUT /visitors/{id}/decision`, `POST /visitors/scan` |
| Reports | `GET /reports`, `GET /reports/{key}`, `GET /reports/hostel-cursor-report` |

## 12. Tests and evidence

With the database, backend and frontend running:

```bash
python3 -m venv .venv && .venv/bin/pip install bcrypt oracledb reportlab playwright pillow
.venv/bin/python -m playwright install chromium

.venv/bin/python tools/api_workflow_test.py          # 33 API checks (auth, roles, all workflows)
tools/redis-demo-test.sh                             # cache + rate limit demo (waits 5 min for expiry)
.venv/bin/python tools/capture_screenshots.py        # screenshots of every screen
.venv/bin/python tools/ui_transactions_test.py       # 9 transactions performed through the UI
```

These scripts change data — run `database/reset-db.sh` afterwards to return to the seed state.
Saved outputs are in `docs/evidence/`.

## 13. Reports

- `docs/review1/` — Review 1 (submitted)
- `docs/review2/Harjot_Tanuj_Daksh_DBMS_Review2.pdf` — database implementation
- `docs/final/Harjot_Tanuj_Daksh_DBMS_Final_Project.pdf` — complete integrated system

They are built from real outputs by `tools/build_reports.py` (see that file's header for the steps).

## 14. Known issues / limitations

- No online payment gateway — fees are recorded by the office.
- E-mail notifications are not implemented; students see status changes on their dashboard.
- Wardens can see all hostels (not only their own).
- The login rate limit counts per IP address; many users behind one network address share a counter.
- Uploaded images are stored on the backend's local disk (`backend/uploads/`).

## 15. Troubleshooting

| Symptom | Fix |
|---|---|
| `Cannot connect to the Docker daemon` | Start Docker Desktop. |
| Oracle stays `(health: starting)` | Wait; the first start initialises the database (≈2 min). `docker logs shms-oracle` shows progress. |
| Backend: `ORA-01017 invalid username/password` | Check `database/.env` and that `run-all.sh` was run against the same container. |
| Backend: `Connection refused ... 1521` | Oracle container not running/healthy: `docker compose ps`. |
| Backend: `Schema-validation: missing table` | Run `database/run-all.sh` (tables are created by SQL scripts, not by Hibernate). |
| `Port 8080 already in use` | Stop the other backend run, or set `server.port`. |
| Frontend shows "Cannot reach the server" | Backend not running on 8080, or opened via `127.0.0.1` instead of `localhost` (CORS). |
| Login says "Too many failed login attempts" | Wait for the time shown (≤5 min) or `docker exec shms-redis redis-cli DEL rate:login:fail:<ip>`. |
| Redis down | App still works (reads Oracle, logins allowed); start it with `docker compose up -d redis`. |

Real problems met during development and how they were solved: [docs/development-log.md](docs/development-log.md).
