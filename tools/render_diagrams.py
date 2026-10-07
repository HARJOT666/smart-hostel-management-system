"""Renders the Mermaid diagrams used in the reports to PNG (headless Chromium + mermaid.js from jsDelivr)."""
from pathlib import Path

from playwright.sync_api import sync_playwright

OUT = Path(__file__).resolve().parent.parent / "docs" / "evidence" / "diagrams"

ER_CORE = """
erDiagram
    HOSTEL ||--|{ ROOM : contains
    HOSTEL ||--|{ STAFF : employs
    STUDENT ||--o{ ROOM_ALLOCATION : receives
    ROOM ||--o{ ROOM_ALLOCATION : "assigned in"
    STAFF ||--o{ ROOM_ALLOCATION : allocates
    STUDENT ||--o{ COMPLAINT : raises
    STAFF |o--o{ COMPLAINT : handles
    STUDENT ||--o{ LEAVE_REQUEST : "applies for"
    STAFF |o--o{ LEAVE_REQUEST : approves
    STUDENT ||--o{ VISITOR_PASS : requests
    STAFF |o--o{ VISITOR_PASS : authorizes
    HOSTEL { number hostel_id PK
             varchar hostel_name UK
             varchar gender_type
             varchar location
             number total_floors }
    ROOM { number room_id PK
           varchar room_number
           number floor_no
           varchar room_type
           number capacity
           number occupied_count
           number hostel_id FK }
    STUDENT { number student_id PK
              varchar registration_no UK
              varchar name
              varchar email UK
              varchar phone
              varchar gender
              number year
              varchar branch
              number cgpa
              varchar password_hash }
    STAFF { number staff_id PK
            varchar name
            varchar role
            varchar specialization
            varchar phone UK
            varchar email UK
            number hostel_id FK
            varchar password_hash }
    ROOM_ALLOCATION { number allocation_id PK
                      number student_id FK
                      number room_id FK
                      number allocated_by FK
                      number bed_number
                      date allocation_date
                      date vacate_date
                      varchar status }
    COMPLAINT { number complaint_id PK
                number student_id FK
                number assigned_to FK
                varchar category
                varchar description
                varchar priority
                varchar status
                timestamp created_date
                timestamp resolved_date
                varchar remarks
                date scheduled_date
                varchar image_path
                varchar completion_image_path }
    LEAVE_REQUEST { number leave_id PK
                    number student_id FK
                    number approved_by FK
                    date start_date
                    date end_date
                    varchar reason
                    varchar destination
                    date applied_date
                    varchar status }
    VISITOR_PASS { number pass_id PK
                   number student_id FK
                   number approved_by FK
                   varchar visitor_name
                   varchar visitor_phone
                   varchar relation_to_student
                   varchar purpose
                   date visit_date
                   timestamp entry_time
                   timestamp exit_time
                   varchar qr_code UK
                   varchar status }
"""

ER_EXT = """
erDiagram
    STUDENT ||--o{ FEE_PAYMENT : "is billed"
    STAFF ||--o{ ANNOUNCEMENT : posts
    HOSTEL |o--o{ ANNOUNCEMENT : "targets"
    STUDENT ||--o{ MARKETPLACE_LISTING : sells
    STUDENT |o--o{ MARKETPLACE_LISTING : buys
    HOSTEL ||--o{ INVENTORY_ITEM : stocks
    STUDENT ||--o{ LOST_FOUND_ITEM : reports
    STUDENT |o--o{ LOST_FOUND_ITEM : claims
    HOSTEL ||--o{ MESS_MENU : serves
"""

ARCH = """
flowchart LR
    subgraph B[Browser]
        R["React app<br/>Vite · React Router · Axios"]
    end
    subgraph S["Spring Boot 3 REST API (:8080)"]
        F["JWT filter +<br/>role checks"] --> C[Controllers] --> SV["Services<br/>@Transactional<br/>@Cacheable / @CacheEvict"]
        SV --> J["JPA repositories"]
        SV --> P["PlsqlRepository<br/>ReportRepository<br/>(JdbcTemplate)"]
        L["Login + rate limiter"]
    end
    subgraph D["Docker"]
        O[("Oracle 23ai Free<br/>tables · constraints<br/>procedures · functions<br/>cursors · triggers")]
        X[("Redis 7<br/>cache + login counters")]
    end
    R -- "JSON + Bearer JWT" --> F
    J -- JDBC --> O
    P -- "JDBC: CALL / SELECT" --> O
    SV -. "cache" .-> X
    L -. "INCR / EXPIRE" .-> X
"""

SEQ = """
sequenceDiagram
    participant UI as React UI
    participant API as AllocationController
    participant SV as AllocationService
    participant DB as Oracle
    UI->>API: POST /api/allocations {studentId, roomId} + JWT
    API->>SV: allocate() [role ADMIN/WARDEN checked]
    SV->>DB: CALL sp_allocate_room(student, room, staff, OUT id, OUT bed)
    DB->>DB: verify student, staff, room (row lock), gender, fn_available_beds
    DB->>DB: fn_find_free_bed → INSERT ROOM_ALLOCATION
    DB->>DB: trg_allocation_validate, trg_allocation_occupancy (+1)
    DB-->>SV: allocation_id, bed_number  (or ORA-200xx → rollback)
    SV-->>API: AllocationResponse (cache dashboardStats evicted)
    API-->>UI: 201 Created → "Allocated … bed 2"
"""

PAGE = """<!doctype html><html><head><meta charset="utf-8">
<style>body{margin:0;background:#fff;font-family:'Liberation Sans',Arial,sans-serif} #d{display:inline-block;padding:16px}</style>
<script src="https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js"></script></head>
<body><div id="d"><pre class="mermaid">%s</pre></div>
<script>
mermaid.initialize({startOnLoad:false, theme:'base', er:{useMaxWidth:false}, flowchart:{useMaxWidth:false}, sequence:{useMaxWidth:false}, themeVariables:{
  fontFamily:'Liberation Sans, Arial, sans-serif', fontSize:'15px',
  primaryColor:'#f4ece2', primaryBorderColor:'#7b4329', primaryTextColor:'#2b2420',
  lineColor:'#5a4d43', secondaryColor:'#fbf8f3', tertiaryColor:'#f0e9de',
  attributeBackgroundColorOdd:'#fbf8f3', attributeBackgroundColorEven:'#f4ece2'}});
mermaid.run().then(()=>document.body.setAttribute('data-done','1'));
</script></body></html>"""


def render(page, name, src, width=1600):
    page.set_viewport_size({"width": width, "height": 800})
    page.set_content(PAGE % src)
    page.wait_for_selector("body[data-done='1']", timeout=30000)
    page.locator("#d").screenshot(path=str(OUT / f"{name}.png"))
    print("rendered", name)


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page(device_scale_factor=2)
        render(pg, "er_core", ER_CORE, 2000)
        render(pg, "er_extension", ER_EXT, 1400)
        render(pg, "architecture", ARCH, 1700)
        render(pg, "allocation_sequence", SEQ, 1400)
        b.close()
