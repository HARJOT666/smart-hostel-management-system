# Architecture

```mermaid
flowchart LR
    subgraph Browser
        R[React app<br/>Vite · React Router · Axios]
    end
    subgraph Backend["Spring Boot 3 (IntelliJ, :8080)"]
        F[JwtAuthenticationFilter] --> C[Controllers<br/>@PreAuthorize roles]
        C --> S[Services<br/>@Transactional · @Cacheable/@CacheEvict]
        S --> J[Spring Data JPA repositories]
        S --> P[PlsqlRepository / ReportRepository<br/>JdbcTemplate]
        A[AuthService + LoginRateLimiter]
    end
    subgraph Docker
        O[(Oracle 23ai Free<br/>user1 @ FREEPDB1<br/>tables · constraints · PL/SQL)]
        X[(Redis 7<br/>cache + login counters)]
    end
    R -- "HTTP JSON + Bearer JWT" --> F
    J -- JDBC --> O
    P -- "JDBC: CALL procedures, functions" --> O
    S -. "cache read/evict" .-> X
    A -. "INCR / EXPIRE / DEL" .-> X
```

## Layers

| Layer | Responsibility | Where |
|---|---|---|
| Presentation | Screens per role, forms, tables, charts; calls the REST API with the JWT | `frontend/src` |
| Security | Verify JWT on each request, role checks, BCrypt password check at login, login rate limit | `backend/.../security` |
| Controller | Map HTTP to Java; validate input (`@Valid`); no business logic | `backend/.../controller` |
| Service | Business rules and transactions; caching annotations | `backend/.../service` |
| Repository | JPA for CRUD/search; JdbcTemplate for PL/SQL calls and report SQL | `backend/.../repository` |
| Database | Data, integrity constraints, procedures, functions, cursors, triggers | Oracle (`database/`) |
| Cache | Copies of slow-changing data; temporary counters | Redis |

## Request example: allocate a room

```mermaid
sequenceDiagram
    participant UI as React (Room allocation page)
    participant API as AllocationController
    participant SV as AllocationService (@Transactional)
    participant DB as Oracle
    UI->>API: POST /api/allocations {studentId, roomId} + JWT
    API->>API: JwtAuthenticationFilter + @PreAuthorize(ADMIN, WARDEN)
    API->>SV: allocate(request, staffId from token)
    SV->>DB: {call sp_allocate_room(?,?,?,?,?)}
    DB->>DB: checks + FOR UPDATE lock + fn_available_beds + fn_find_free_bed
    DB->>DB: INSERT room_allocation → trg_allocation_validate, trg_allocation_occupancy
    DB-->>SV: OUT allocation_id, bed_number (or ORA-200xx → rollback)
    SV->>DB: SELECT allocation with student/room/hostel
    SV-->>API: AllocationResponse; dashboardStats cache evicted
    API-->>UI: 201 Created
```

## Authentication

1. `POST /api/auth/login` → `LoginRateLimiter.checkAllowed(ip)` (Redis) → `AuthenticationManager` →
   `CustomUserDetailsService` (STAFF by e-mail / STUDENT by registration no.) → BCrypt compare.
2. Success → `JwtService` issues an HS256 token (sub, uid, role, name, exp = 8 h).
3. Later requests → `JwtAuthenticationFilter` validates the token and sets the user in the security context;
   `@PreAuthorize` checks the role. Services use the user id from the token for "my" data.

## Why these choices

- **PL/SQL for the critical rules** (allocation, assignment, leave decision, occupancy): the rules hold even if
  data is changed outside the app, and the DBMS course requires them. Spring calls them inside its transaction.
- **Hibernate `ddl-auto: validate`**: the SQL scripts own the schema; Hibernate only checks the entities match.
- **Redis is optional at runtime**: cache errors fall back to Oracle; the rate limiter fails open.
- **Stateless JWT**: no server sessions; simple to reason about.
