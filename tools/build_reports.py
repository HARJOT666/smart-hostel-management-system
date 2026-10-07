"""
Builds the two Database Systems reports from REAL project evidence:

  docs/review2/Harjot_Tanuj_Daksh_DBMS_Review2.pdf
  docs/final/Harjot_Tanuj_Daksh_DBMS_Final_Project.pdf

Nothing in the reports is typed in by hand as "output": query results, PL/SQL output, row counts,
constraint statistics, test results and screenshots are read from docs/evidence/, which is produced by:

  1. database/reset-db.sh                          (fresh seeded database)
  2. tools/capture_screenshots.py                   (screens of every page)
  3. tools/collect_db_evidence.py                   (queries, PL/SQL output, counts ...)
  4. tools/render_diagrams.py                       (ER / architecture diagrams)
  5. tools/api_workflow_test.py, tools/redis-demo-test.sh, tools/ui_transactions_test.py (tests)
  6. .venv/bin/python tools/build_reports.py [review2|final]

"Problems Encountered" is taken from docs/development-log.md.
"""
import json
import re
import sys
from html import escape
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (BaseDocTemplate, CondPageBreak, Frame, Image, KeepTogether, PageBreak, PageTemplate,
                                Paragraph, Preformatted, Spacer, Table, TableStyle)

ROOT = Path(__file__).resolve().parent.parent
EV = ROOT / "docs" / "evidence"
DB = EV / "db"
SHOTS = EV / "screenshots"
TX = EV / "transactions"
DIAG = EV / "diagrams"

TEAM_DBMS = [("Harjot Singh Bagga", "24BYB0115"), ("Tanuj Sharma", "24BYB0122"), ("Daksh Khanna", "24BYB0125")]

# ---------------------------------------------------------------- fonts & styles
FONT_DIR = Path("/usr/share/fonts/truetype/liberation")
for name, file in [("Serif", "LiberationSerif-Regular"), ("Serif-Bold", "LiberationSerif-Bold"),
                   ("Serif-Italic", "LiberationSerif-Italic"), ("Serif-BoldItalic", "LiberationSerif-BoldItalic"),
                   ("Sans", "LiberationSans-Regular"), ("Sans-Bold", "LiberationSans-Bold"),
                   ("Mono", "LiberationMono-Regular"), ("Mono-Bold", "LiberationMono-Bold")]:
    pdfmetrics.registerFont(TTFont(name, str(FONT_DIR / f"{file}.ttf")))
pdfmetrics.registerFontFamily("Serif", normal="Serif", bold="Serif-Bold", italic="Serif-Italic", boldItalic="Serif-BoldItalic")
pdfmetrics.registerFontFamily("Mono", normal="Mono", bold="Mono-Bold", italic="Mono", boldItalic="Mono-Bold")

PAGE_W, PAGE_H = A4
LM = RM = 22 * mm
CONTENT_W = PAGE_W - LM - RM
INK = colors.HexColor("#1f1f1f")
GREY = colors.HexColor("#555555")
HEAD_BG = colors.HexColor("#e6e6e6")
CODE_BG = colors.HexColor("#f4f4f2")
LINE = colors.HexColor("#9a9a9a")

S = {
    "body": ParagraphStyle("body", fontName="Serif", fontSize=10.5, leading=14.5, alignment=TA_JUSTIFY, spaceAfter=6, textColor=INK),
    "h1": ParagraphStyle("h1", fontName="Serif-Bold", fontSize=15, leading=19, spaceBefore=4, spaceAfter=10, textColor=INK),
    "h2": ParagraphStyle("h2", fontName="Serif-Bold", fontSize=12, leading=15, spaceBefore=8, spaceAfter=5, textColor=INK, keepWithNext=1),
    "h3": ParagraphStyle("h3", fontName="Serif-BoldItalic", fontSize=10.5, leading=13.5, spaceBefore=5, spaceAfter=3, textColor=INK, keepWithNext=1),
    "bullet": ParagraphStyle("bullet", fontName="Serif", fontSize=10.5, leading=14, leftIndent=14, bulletIndent=4, spaceAfter=2, alignment=TA_JUSTIFY),
    "cell": ParagraphStyle("cell", fontName="Serif", fontSize=9, leading=11),
    "cellb": ParagraphStyle("cellb", fontName="Serif-Bold", fontSize=9, leading=11),
    "dcell": ParagraphStyle("dcell", fontName="Sans", fontSize=7, leading=8.4),
    "dhead": ParagraphStyle("dhead", fontName="Sans-Bold", fontSize=7, leading=8.4),
    "code": ParagraphStyle("code", fontName="Mono", fontSize=7.3, leading=9, backColor=CODE_BG, borderPadding=(4, 4, 4, 4),
                           leftIndent=4, rightIndent=4, spaceBefore=4, spaceAfter=8),
    "caption": ParagraphStyle("caption", fontName="Serif-Italic", fontSize=9, leading=11, alignment=TA_CENTER, spaceBefore=3, spaceAfter=10, textColor=GREY),
    "small": ParagraphStyle("small", fontName="Serif-Italic", fontSize=9, leading=11.5, textColor=GREY, spaceAfter=6),
    "c_center": ParagraphStyle("c_center", fontName="Serif", fontSize=11, leading=14, alignment=TA_CENTER),
}


def P(text, style="body"):
    return Paragraph(text, S[style])


def bullets(items):
    return [Paragraph(t, S["bullet"], bulletText="•") for t in items]


def code(text, max_lines=None):
    lines = text.rstrip("\n").splitlines()
    if max_lines and len(lines) > max_lines:
        lines = lines[:max_lines] + [f"... ({len(lines) - max_lines} more lines in the script file)"]
    return Preformatted("\n".join(lines), S["code"], maxLineLength=112, newLineChars="  ")


def table(rows, widths, header=True, font_style="cell", zebra=False):
    data = [[c if not isinstance(c, str) else Paragraph(c, S["cellb" if header and i == 0 else font_style])
             for c in row] for i, row in enumerate(rows)]
    t = Table(data, colWidths=widths, repeatRows=1 if header else 0)
    style = [("GRID", (0, 0), (-1, -1), 0.5, LINE), ("VALIGN", (0, 0), (-1, -1), "TOP"),
             ("TOPPADDING", (0, 0), (-1, -1), 3), ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
             ("LEFTPADDING", (0, 0), (-1, -1), 5), ("RIGHTPADDING", (0, 0), (-1, -1), 5)]
    if header:
        style.append(("BACKGROUND", (0, 0), (-1, 0), HEAD_BG))
    t.setStyle(TableStyle(style))
    return t


def data_table(result, max_rows=12):
    """Renders a captured query result ({columns, rows}) as a compact table."""
    if "message" in result:
        return P(f"<i>Result:</i> {escape(result['message'])}", "body")
    cols, rows = result["columns"], result["rows"][:max_rows]
    values = [max([len(str(r[i])) for r in rows] or [1]) for i in range(len(cols))]
    longest_word = [max(len(w) for w in c.split("_")) for c in cols]
    lengths = [max(min(v, 38), longest_word[i] + 1, 4) for i, v in enumerate(values)]
    total = sum(lengths)
    widths = [CONTENT_W * l / total for l in lengths]
    head = [Paragraph(escape(c.lower().replace("_", " ")), S["dhead"]) for c in cols]
    body = [[Paragraph(escape("" if v is None else str(v)), S["dcell"]) for v in r] for r in rows]
    t = Table([head] + body, colWidths=widths, repeatRows=1)
    t.setStyle(TableStyle([("GRID", (0, 0), (-1, -1), 0.4, LINE), ("BACKGROUND", (0, 0), (-1, 0), HEAD_BG),
                           ("VALIGN", (0, 0), (-1, -1), "TOP"), ("TOPPADDING", (0, 0), (-1, -1), 1.5),
                           ("BOTTOMPADDING", (0, 0), (-1, -1), 1.5), ("LEFTPADDING", (0, 0), (-1, -1), 3),
                           ("RIGHTPADDING", (0, 0), (-1, -1), 3)]))
    return t


def project(result, keep):
    """Only the listed columns of a captured result."""
    idx = [result["columns"].index(c) for c in keep]
    return {"columns": keep, "rows": [[r[i] for i in idx] for r in result["rows"]]}


SAMPLE_COLUMNS = {
    "STUDENT": ["STUDENT_ID", "REGISTRATION_NO", "NAME", "GENDER", "YEAR", "BRANCH", "CGPA", "PASSWORD_HASH"],
    "STAFF": ["STAFF_ID", "NAME", "ROLE", "SPECIALIZATION", "PHONE", "EMAIL", "HOSTEL_ID"],
    "COMPLAINT": ["COMPLAINT_ID", "STUDENT_ID", "ASSIGNED_TO", "CATEGORY", "PRIORITY", "STATUS", "CREATED_DATE", "RESOLVED_DATE", "SCHEDULED_DATE"],
    "LEAVE_REQUEST": ["LEAVE_ID", "STUDENT_ID", "APPROVED_BY", "START_DATE", "END_DATE", "REASON", "DESTINATION", "STATUS"],
    "VISITOR_PASS": ["PASS_ID", "STUDENT_ID", "APPROVED_BY", "VISITOR_NAME", "RELATION_TO_STUDENT", "VISIT_DATE", "QR_CODE", "STATUS"],
}


def image(path, width=CONTENT_W, caption=None, max_h=200 * mm):
    from PIL import Image as PILImage
    w, h = PILImage.open(path).size
    draw_w = width
    draw_h = draw_w * h / w
    if draw_h > max_h:
        draw_h = max_h
        draw_w = draw_h * w / h
    items = [Image(str(path), width=draw_w, height=draw_h)]
    if caption:
        items.append(P(caption, "caption"))
    return KeepTogether(items)


class Heading(Paragraph):
    """A numbered section heading that is recorded for the index."""

    def __init__(self, text, key):
        super().__init__(text, S["h1"])
        self.toc_key = key


# ---------------------------------------------------------------- document template
class ReportDoc(BaseDocTemplate):
    def __init__(self, path, header_right, footer_left, **kw):
        super().__init__(str(path), pagesize=A4, leftMargin=LM, rightMargin=RM, topMargin=24 * mm,
                         bottomMargin=20 * mm, title=kw.pop("title"), author=kw.pop("author"))
        self.header_right, self.footer_left = header_right, footer_left
        self.section_pages = {}
        frame = Frame(LM, 20 * mm, CONTENT_W, PAGE_H - 44 * mm, id="f")
        cover_frame = Frame(LM, 20 * mm, CONTENT_W, PAGE_H - 40 * mm, id="c")
        self.addPageTemplates([PageTemplate("cover", [cover_frame], onPage=self.draw_cover_border),
                               PageTemplate("body", [frame], onPage=self.draw_header_footer)])

    def afterFlowable(self, flowable):
        if isinstance(flowable, Heading):
            self.section_pages.setdefault(flowable.toc_key, self.page)

    def draw_cover_border(self, canvas, doc):
        canvas.saveState()
        canvas.setStrokeColor(INK)
        canvas.setLineWidth(1.2)
        canvas.rect(12 * mm, 12 * mm, PAGE_W - 24 * mm, PAGE_H - 24 * mm)
        canvas.setLineWidth(0.4)
        canvas.rect(14 * mm, 14 * mm, PAGE_W - 28 * mm, PAGE_H - 28 * mm)
        canvas.restoreState()

    def draw_header_footer(self, canvas, doc):
        canvas.saveState()
        canvas.setFont("Serif-Italic", 8.5)
        canvas.setFillColor(GREY)
        top = PAGE_H - 14 * mm
        canvas.drawString(LM, top, "Smart Hostel Management System")
        canvas.drawRightString(PAGE_W - RM, top, self.header_right)
        canvas.setStrokeColor(LINE)
        canvas.setLineWidth(0.5)
        canvas.line(LM, top - 3, PAGE_W - RM, top - 3)
        canvas.line(LM, 15 * mm, PAGE_W - RM, 15 * mm)
        canvas.drawString(LM, 11 * mm, self.footer_left)
        canvas.setFont("Serif", 9)
        canvas.drawRightString(PAGE_W - RM, 11 * mm, str(doc.page))
        canvas.restoreState()


def cover(review_title, subtitle):
    from reportlab.platypus import NextPageTemplate
    items = [Spacer(1, 14 * mm), image(ROOT / "docs/assets/vit-logo.png", width=78 * mm), Spacer(1, 6 * mm),
             P("<b>School of Computer Science Engineering and Information Systems</b>", "c_center"),
             P("B.Tech (Cybersecurity)", "c_center"), Spacer(1, 16 * mm),
             Paragraph("DATABASE SYSTEMS LAB", ParagraphStyle("ct", fontName="Serif-Bold", fontSize=18, leading=22, alignment=TA_CENTER)),
             P("BCSE302L", "c_center"), Spacer(1, 16 * mm),
             Paragraph(review_title, ParagraphStyle("rt", fontName="Serif-Bold", fontSize=16, leading=20, alignment=TA_CENTER)),
             Paragraph(subtitle, ParagraphStyle("st", fontName="Serif-Italic", fontSize=12, leading=16, alignment=TA_CENTER)),
             Spacer(1, 16 * mm), P("Project Title", "c_center"),
             Paragraph("Smart Hostel Management System", ParagraphStyle("pt", fontName="Serif-Bold", fontSize=20, leading=26, alignment=TA_CENTER)),
             Spacer(1, 22 * mm)]
    team = Table([[Paragraph("<b>Team Members</b>", S["cell"]), ""]] +
                 [[Paragraph(n, S["cell"]), Paragraph(r, S["cell"])] for n, r in TEAM_DBMS],
                 colWidths=[60 * mm, 40 * mm], hAlign="CENTER")
    team.setStyle(TableStyle([("LINEBELOW", (0, 0), (-1, 0), 0.8, INK), ("TOPPADDING", (0, 0), (-1, -1), 4),
                              ("BOTTOMPADDING", (0, 0), (-1, -1), 4)]))
    items += [team, NextPageTemplate("body"), PageBreak()]
    return items


def index_page(sections, pages):
    rows = [["S. No.", "Component", "Page Number"]]
    keys = [k for k, _ in sections]
    for i, (key, title) in enumerate(sections):
        start = pages.get(key, 0)
        nxt = pages.get(keys[i + 1], start + 1) - 1 if i + 1 < len(keys) else pages.get("__end__", start)
        rng = f"{start}" if nxt <= start else f"{start} – {nxt}"
        rows.append([str(i + 1), title, rng])
    return [Paragraph("Index", ParagraphStyle("ix", fontName="Serif-Bold", fontSize=15, leading=19, alignment=TA_CENTER, spaceAfter=12)),
            table(rows, [16 * mm, 110 * mm, 30 * mm]), PageBreak()]


def build(path, header_right, footer_left, title, cover_items, sections, section_builders):
    """Two passes: the first finds the page of every section, the second prints the index with those pages."""
    pages = {}
    for _ in range(2):
        doc = ReportDoc(path, header_right, footer_left, title=title,
                        author="Harjot Singh Bagga, Tanuj Sharma, Daksh Khanna")
        story = list(cover_items) + index_page(sections, pages)
        for n, (key, heading) in enumerate(sections, start=1):
            story.append(Heading(f"{n}. {heading}", key))
            story += section_builders[key]()
            story += [Spacer(1, 10), CondPageBreak(95 * mm)]   # new page only if little space is left
        doc.build(story)
        pages = dict(doc.section_pages)
        pages["__end__"] = doc.page
    print(f"built {path.relative_to(ROOT)} ({doc.page} pages)")


# ---------------------------------------------------------------- evidence loaders
def load(name):
    return json.loads((DB / name).read_text())


def demo_section(tag):
    """Lines of one block (e.g. 'T1') from the captured SQL*Plus demo output."""
    text = (DB / "demo_transactions.txt").read_text()
    m = re.search(rf"^===== {tag}\..*?$(.*?)(?=^===== T|\Z)", text, re.S | re.M)
    block = m.group(0) if m else ""
    lines = [l for l in block.splitlines() if "PL/SQL procedure successfully" not in l and l.strip()
             and not l.startswith("All demo") and not l.startswith("Rollback")]
    return "\n".join(lines)


def sql_object(file, name):
    """Source of one CREATE OR REPLACE ... block from a script."""
    text = (DB / "sql_files" / file).read_text()
    m = re.search(rf"((?:--[^\n]*\n)*)CREATE OR REPLACE \w+ {name}\b.*?^/\s*$", text, re.S | re.M | re.I)
    return m.group(0).rstrip("/\n ").rstrip() + "\n/" if m else f"-- {name} not found"


def dev_log_entries():
    text = (ROOT / "docs/development-log.md").read_text()
    entries = []
    for m in re.finditer(r"^## (\d+)\. (.+?)\n(.*?)(?=^## \d+\.|\Z)", text, re.S | re.M):
        num, title, body = m.groups()

        def field(*names):
            for n in names:
                f = re.search(rf"- \*\*{n}[^*]*:\*\*\s*(.*?)(?=\n- \*\*|\Z)", body, re.S)
                if f:
                    return re.sub(r"\s+", " ", f.group(1)).strip()
            return ""
        err = re.search(r"```\n(.*?)```", body, re.S)
        entries.append({"num": num, "title": title.strip(), "date": field("Date"),
                        "problem": field("Problem", "Problems found", "Problems and fixes"),
                        "cause": field("Cause"), "fix": field("Fix", "Fixes", "What was tried / fixed"),
                        "error": err.group(1).strip() if err else ""})
    return entries


def md_inline(text):
    """Minimal Markdown -> ReportLab markup (`code`, **bold**)."""
    t = escape(text)
    t = re.sub(r"`([^`]+)`", r'<font name="Mono" size="8.5">\1</font>', t)
    t = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", t)
    return t


# ---------------------------------------------------------------- shared sections
QUERY_PURPOSE = {
    "Q1": "Warden's resident list for one hostel, room by room.",
    "Q2": "Rooms that can still take students (used before allocation).",
    "Q3": "Hostel occupancy report for the administration.",
    "Q4": "Which kinds of problems occur most and how many are still pending.",
    "Q5": "Categories that take too long to fix (service-level monitoring).",
    "Q6": "Overdue complaints, with who is handling them, for follow-up.",
    "Q7": "Workload of each maintenance worker, for fair assignment.",
    "Q8": "Warden's queue of leave requests to decide.",
    "Q9": "Approval behaviour of each warden (audit).",
    "Q10": "Gate list of visitors expected this week.",
    "Q11": "Monthly visitor statistics.",
    "Q12": "Students still waiting for a room.",
    "Q13": "Students raising unusually many complaints (room may need inspection).",
    "Q14": "Fully occupied rooms in the busiest hostel.",
    "Q15": "Which room types are most in demand.",
    "Q16": "Fee defaulters for the current semester.",
    "Q17": "Hostel staff directory / emergency contacts.",
    "Q18": "Branch-wise residents and academic profile.",
    "Q19": "New student registration (INSERT).",
    "Q20": "Rescheduling assigned maintenance jobs (UPDATE with subquery).",
    "Q21": "Clearing old withdrawn marketplace listings (DELETE).",
}


def sec_introduction():
    return [
        P("<b>Problem statement.</b> In most institutions hostel operations still run on paper registers and separate "
          "spreadsheets. Room allotment, maintenance complaints, leave applications and visitor entries are recorded in "
          "different places and are not linked to each other, so vacancy information is unreliable, complaints have no "
          "clear owner, approvals are hard to trace and reports need manual effort."),
        P("<b>Proposed system.</b> The Smart Hostel Management System (SHMS) stores all hostel data in one Oracle "
          "database and exposes it through a Spring Boot REST API and a React web application for three kinds of users: "
          "students, wardens/administrators and maintenance staff. The database is the core of the system: business "
          "rules are enforced by constraints, the main operations are implemented as PL/SQL procedures, and triggers "
          "keep derived data (room occupancy, resolution time, visitor QR codes) correct automatically."),
        P("This review documents the <b>implementation</b> of the database designed in Review 1: the tables and constraints "
          "that were created, the data that was loaded, the SQL queries, the procedures, functions, cursors and triggers, "
          "and how the application uses them in real transactions. All outputs in this document were produced by "
          "running the scripts against the project database (Oracle Database 23ai Free in Docker, schema <font name='Mono'>user1</font>)."),
        P("Objectives", "h2"),
        *bullets(["Maintain a central database of hostels, rooms, students and staff.",
                  "Allocate beds safely: never over capacity, never two active rooms for one student, never the wrong gender hostel.",
                  "Track complaints from registration to resolution, with the responsible staff member.",
                  "Digitise leave applications and visitor passes with approval records and QR-based entry.",
                  "Produce occupancy, complaint, workload, leave, visitor and fee reports directly from the database."]),
    ]


def sec_functional_requirements():
    rows = [["ID", "Requirement", "Description", "Main tables"],
            ["FR-01", "Login & registration", "Students (registration no.) and staff (e-mail) log in; passwords stored as BCrypt hashes; students can self-register.", "STUDENT, STAFF"],
            ["FR-02", "Room allocation & vacancy", "Admin/warden allots a bed to a waiting student; vacancy shown per room; rooms can be vacated with history kept.", "ROOM_ALLOCATION, ROOM, HOSTEL"],
            ["FR-03", "Complaint management", "Students raise complaints; warden assigns them to matching maintenance staff; staff update status until resolved.", "COMPLAINT, STAFF"],
            ["FR-04", "Leave management", "Students apply for leave; warden approves or rejects; approval letter is generated.", "LEAVE_REQUEST"],
            ["FR-05", "Visitor management", "Students request visitor passes; warden approves; a unique QR code is used for entry and exit.", "VISITOR_PASS"],
            ["FR-06", "Fee tracking", "Students see dues and payment history; office records payments.", "FEE_PAYMENT (extension)"],
            ["FR-07", "Announcements", "Wardens post notices for one hostel or all hostels.", "ANNOUNCEMENT (extension)"],
            ["FR-08", "Marketplace", "Students list items for sale and mark them sold.", "MARKETPLACE_LISTING (extension)"],
            ["FR-09", "Inventory", "Hostel items with total and damaged quantities.", "INVENTORY_ITEM (extension)"],
            ["FR-10", "Reports & dashboard", "Occupancy, complaints, workload, leave, visitor and fee reports.", "all tables"],
            ["—", "Lost & found, mess menu, emergency contacts", "Supporting hostel-life modules.", "LOST_FOUND_ITEM, MESS_MENU, STAFF"]]
    return [P("The functional requirements come from the Software Engineering project document (FR-01 to FR-10) "
              "and the Review 1 requirement analysis."),
            table(rows, [13 * mm, 35 * mm, 82 * mm, 36 * mm]),
            Spacer(1, 6),
            P("Roles", "h2"),
            table([["Role", "What the role can do"],
                   ["Student", "View own room and roommates, raise/track complaints, apply for leave, request visitor passes, view fees, notices, mess menu, marketplace, lost & found, emergency contacts."],
                   ["Admin / Warden", "Manage students, staff, hostels and rooms; allocate and vacate rooms; assign complaints (manually or automatically); decide leave and visitor requests; gate check-in; record fee payments; notices, mess menu, inventory; reports."],
                   ["Maintenance", "See assigned complaints, schedule visits, update status to In Progress / Resolved, upload completion photos, view inventory."]],
                  [30 * mm, 136 * mm])]


def sec_final_design():
    return [
        P("The final design is the eight-table core schema approved in Review 1 (Figure 1). During implementation the "
          "following refinements were made; they add attributes or stricter constraints but do not change any entity or relationship:"),
        table([["Refinement", "Reason"],
               ["<font name='Mono'>STUDENT.password_hash</font>, <font name='Mono'>STAFF.password_hash</font>", "Application login; only a BCrypt hash is stored."],
               ["<font name='Mono'>COMPLAINT.scheduled_date, image_path, completion_image_path</font>", "Maintenance scheduling and photo upload (SE requirement)."],
               ["<font name='Mono'>STAFF.email</font> NOT NULL", "E-mail is the staff login username."],
               ["CHECK room type ↔ capacity; CHECK role ↔ specialization", "Single = 1 bed … Quad = 4 beds; only maintenance staff have a specialization."],
               ["Function-based UNIQUE indexes on ROOM_ALLOCATION", "Implements Review 1's rule \"at most one Active allocation per student and per bed\" so it can never be bypassed."]],
              [78 * mm, 88 * mm]),
        Spacer(1, 8),
        image(DIAG / "er_core.png", caption="Figure 1: ER diagram of the core schema (crow's-foot notation, PK/FK/UK marked)", max_h=150 * mm),
        P("Relationships and cardinality", "h2"),
        table([["Relationship", "Cardinality", "Foreign key"],
               ["HOSTEL contains ROOM", "1 : N (total)", "ROOM.hostel_id"],
               ["HOSTEL employs STAFF", "1 : N (total)", "STAFF.hostel_id"],
               ["STUDENT receives ROOM_ALLOCATION", "1 : N", "ROOM_ALLOCATION.student_id"],
               ["ROOM assigned in ROOM_ALLOCATION", "1 : N", "ROOM_ALLOCATION.room_id"],
               ["STAFF allocates ROOM_ALLOCATION", "1 : N", "ROOM_ALLOCATION.allocated_by"],
               ["STUDENT raises COMPLAINT", "1 : N", "COMPLAINT.student_id"],
               ["STAFF handles COMPLAINT", "0..1 : N", "COMPLAINT.assigned_to (NULL until assigned)"],
               ["STUDENT applies for LEAVE_REQUEST", "1 : N", "LEAVE_REQUEST.student_id"],
               ["STAFF approves LEAVE_REQUEST", "0..1 : N", "LEAVE_REQUEST.approved_by"],
               ["STUDENT requests VISITOR_PASS", "1 : N", "VISITOR_PASS.student_id"],
               ["STAFF authorizes VISITOR_PASS", "0..1 : N", "VISITOR_PASS.approved_by"]],
              [70 * mm, 30 * mm, 66 * mm]),
        P("Application extension tables", "h2"),
        P("Six further tables support Software Engineering modules (fees, notices, marketplace, inventory, lost &amp; found, "
          "mess menu). They are <b>not</b> part of the 6–8 table DBMS core; each only references a core table, so the core "
          "relations are unchanged (Figure 2)."),
        image(DIAG / "er_extension.png", caption="Figure 2: Extension tables and the core tables they reference", max_h=40 * mm),
        P("Normal form", "h2"),
        P("Every table has a single-column surrogate primary key and every non-key attribute depends only on that key, so "
          "the schema is in 3NF. <font name='Mono'>ROOM.occupied_count</font> is the only stored derived value; it is "
          "maintained exclusively by a trigger. Fee status is deliberately <i>not</i> stored (it is derived from "
          "<font name='Mono'>amount_paid</font> and <font name='Mono'>amount_due</font>), which avoids a transitive dependency."),
    ]


def sec_table_creation():
    core = (DB / "sql_files/01_core_schema.sql").read_text()
    return [P("Tables are created by <font name='Mono'>database/schema/01_core_schema.sql</font>. Primary keys, "
              "NOT NULL and DEFAULT are declared with the columns; identity columns generate the keys. The complete script "
              "for the eight core tables is reproduced below; the extension tables are in "
              "<font name='Mono'>03_extension_tables.sql</font>."),
            code(core),
            P("Execution", "h2"),
            P("All scripts are run in order by <font name='Mono'>database/run-all.sh</font> (schema → constraints → extension "
              "tables → functions → procedures → cursors → triggers → data). Every script ran without errors; the status of "
              "all PL/SQL objects is listed at the end of the Triggers section.")]


def sec_constraints():
    c = load("constraints.json")
    core = ["HOSTEL", "ROOM", "STUDENT", "STAFF", "ROOM_ALLOCATION", "COMPLAINT", "LEAVE_REQUEST", "VISITOR_PASS"]
    rows = [["Table", "PK", "FK", "UNIQUE", "CHECK", "NOT NULL", "DEFAULT"]]
    for t in core:
        d = c["per_table"].get(t, {})
        rows.append([t] + [str(d.get(k, 0)) for k in ["PK", "FK", "UNIQUE", "CHECK", "NOT NULL", "DEFAULT"]])
    checks = [["Constraint", "Table", "Condition"]] + [[escape(a), b, f"<font name='Mono' size='7.5'>{escape(cnd)}</font>"]
                                                      for a, b, cnd in c["checks"] if b in core]
    fks = [["Constraint", "Table.column", "References"]] + [[a, f"{b}.{col}", r] for a, b, col, r in c["foreign_keys"] if b in core]
    cons_sql = (DB / "sql_files/02_core_constraints.sql").read_text()
    return [P("Foreign keys, UNIQUE and CHECK constraints are added by <font name='Mono'>02_core_constraints.sql</font> with "
              "named constraints, so error messages identify the broken rule. The counts below were read from Oracle's data "
              "dictionary (<font name='Mono'>USER_CONSTRAINTS</font>, <font name='Mono'>USER_TAB_COLUMNS</font>) after creation."),
            table(rows, [44 * mm] + [20.3 * mm] * 6),
            P("Table 1: Constraints per core table (from the data dictionary)", "caption"),
            P("Two business rules from Review 1 are implemented as <b>function-based unique indexes</b>: "
              + ", ".join(f"<font name='Mono'>{i}</font>" for i, _ in c["function_based_unique"])
              + ". Only rows with status 'Active' produce a key, so a student can have many old (Vacated) allocations but only one Active one, "
                "and a bed can be held by only one Active allocation."),
            code("CREATE UNIQUE INDEX ux_alloc_active_student ON room_allocation (\n"
                 "    CASE WHEN status = 'Active' THEN student_id END);\n"
                 "CREATE UNIQUE INDEX ux_alloc_active_bed ON room_allocation (\n"
                 "    CASE WHEN status = 'Active' THEN room_id END,\n"
                 "    CASE WHEN status = 'Active' THEN bed_number END);"),
            P("CHECK constraints", "h2"), table(checks, [50 * mm, 36 * mm, 80 * mm], font_style="cell"),
            P("Foreign keys", "h2"), table(fks, [48 * mm, 62 * mm, 56 * mm]),
            P("Constraint enforcement (actual output)", "h2"),
            P("Each statement below violates one constraint; Oracle rejected all of them (block T8 of "
              "<font name='Mono'>10_demo_transactions.sql</font>):"),
            code(demo_section("T8")),
            P("Constraint script (excerpt)", "h2"), code(cons_sql, max_lines=60)]


def sec_population():
    counts = load("counts.json")
    samples = load("samples.json")
    core = ["HOSTEL", "ROOM", "STUDENT", "STAFF", "ROOM_ALLOCATION", "COMPLAINT", "LEAVE_REQUEST", "VISITOR_PASS"]
    rows = [["Table", "Rows", "Type"]] + [[t, str(n), "core" if t in core else "extension"] for t, n in counts["counts"].items()]
    items = [P("<b>Dataset type: SYNTHETIC.</b> No public dataset of hostel operations exists — room, complaint, leave and "
               "visitor records are private student information — so a realistic synthetic dataset was generated by "
               "<font name='Mono'>database/seed/generate_seed.py</font> (fixed random seed, so the same data is produced every "
               "time). Names come from generic Indian first/last-name lists, registration numbers follow the "
               "<font name='Mono'>YYBBBnnnn</font> pattern, e-mails use the fictional domain <font name='Mono'>shms.local</font>, "
               "and no real person's data is used."),
             P("The generator models realistic activity: 6 hostels (4 male, 2 female) with mixed room types, about 72% bed "
               "occupancy with 25 students still waiting, previous-semester allocation history, complaints whose resolution time "
               "depends on the category, leave and visitor requests spread over the semester, and fee records for two semesters. "
               "<font name='Mono'>ROOM.occupied_count</font> is not inserted at all — the occupancy trigger calculated it while the "
               "allocation rows were inserted, and a check query confirmed 0 mismatching rooms."),
             table(rows, [60 * mm, 25 * mm, 30 * mm]),
             P(f"Table 2: Row counts read from the database ({counts['captured']}). Every table has well over 10 records.", "caption"),
             P("Sample rows (first 5 rows of each core table)", "h2")]
    for t in core:
        sample = project(samples[t], SAMPLE_COLUMNS[t]) if t in SAMPLE_COLUMNS else samples[t]
        items += [P(t, "h3"), data_table(sample, 5), Spacer(1, 4)]
    items.append(P("Some wide tables are shown with a subset of their columns. Password hashes are shortened in this listing. All seeded application accounts use the password "
                   "<font name='Mono'>Hostel@123</font>, stored only as a BCrypt hash.", "small"))
    return items


def sec_queries_list():
    q = load("queries.json")["queries"]
    rows = [["No.", "Query (SQL features)", "Business purpose"]]
    for e in q:
        rows.append([e["label"], escape(e["title"]), QUERY_PURPOSE.get(e["label"], "")])
    return [P(f"<font name='Mono'>database/queries/09_queries.sql</font> contains {len(q)} queries. Q1–Q18 retrieve "
              "information and cover filtering, sorting, multi-table joins, outer joins, aggregation, GROUP BY, HAVING, "
              "subqueries (scalar, IN, NOT EXISTS, nested) and calls to stored functions. Q19–Q21 demonstrate INSERT, UPDATE "
              "and DELETE. Several of them are used by the application's Reports page."),
            table(rows, [12 * mm, 84 * mm, 70 * mm])]


def sec_query_outputs():
    q = load("queries.json")
    items = [P(f"Each query was executed with python-oracledb against the seeded database on {q['captured']}. "
               "At most 12 rows of each result are shown. The DML statements (Q19–Q21) were rolled back afterwards.")]
    for e in q["queries"]:
        block = [P(f"{e['label']}. {escape(e['title'])}", "h2"), code(e["sql"])]
        for r in e["results"]:
            block.append(data_table(r, 12))
            block.append(Spacer(1, 3))
            if "rows" in r and len(r["rows"]) == 12:
                block.append(P("(first 12 rows shown)", "small"))
        items += [CondPageBreak(60 * mm)] + block
    return items


def sec_procedures():
    return [P("Four stored procedures implement the main business operations. They do not COMMIT: the caller "
              "(Spring's <font name='Mono'>@Transactional</font> service method, or the demo script) commits or rolls back, "
              "so an error raised anywhere undoes the whole operation."),
            table([["Procedure", "Purpose", "Called by the application"],
                   ["sp_allocate_room", "Validates student, staff role, room (row lock), gender and capacity; picks a free bed; inserts the allocation; returns allocation id and bed (OUT).", "POST /api/allocations"],
                   ["sp_vacate_room", "Closes an Active allocation (status Vacated, vacate date).", "PUT /api/allocations/{id}/vacate"],
                   ["sp_assign_complaint", "Assigns a complaint after checking role, specialization and hostel.", "PUT /api/complaints/{id}/assign"],
                   ["sp_decide_leave", "Pending → Approved/Rejected by a warden or admin only.", "PUT /api/leaves/{id}/decision"]],
                  [36 * mm, 88 * mm, 42 * mm]),
            P("sp_allocate_room", "h2"), code(sql_object("05_procedures.sql", "sp_allocate_room")),
            P("Execution output (T1)", "h3"), code(demo_section("T1")),
            P("sp_vacate_room", "h2"), code(sql_object("05_procedures.sql", "sp_vacate_room")),
            P("Execution output (T2)", "h3"), code(demo_section("T2")),
            P("sp_assign_complaint", "h2"), code(sql_object("05_procedures.sql", "sp_assign_complaint")),
            P("sp_decide_leave", "h2"), code(sql_object("05_procedures.sql", "sp_decide_leave")),
            P("Execution output (T4)", "h3"), code(demo_section("T4"))]


def sec_functions():
    f = load("functions.json")
    return [table([["Function", "Returns", "Used for"],
                   ["fn_available_beds(room_id)", "capacity − occupied", "Vacancy lists, allocation check, available-rooms query (Q2)"],
                   ["fn_hostel_occupancy_pct(hostel_id)", "% of beds occupied", "Dashboard, hostel report, Q3"],
                   ["fn_staff_open_workload(staff_id)", "open jobs of a worker", "Choosing staff for a complaint, auto-assign cursor, Q7"],
                   ["fn_find_free_bed(room_id)", "lowest free bed number", "Inside sp_allocate_room"]],
                  [55 * mm, 40 * mm, 71 * mm]),
            P("fn_available_beds and fn_find_free_bed", "h2"),
            code(sql_object("04_functions.sql", "fn_available_beds")), code(sql_object("04_functions.sql", "fn_find_free_bed")),
            P("Output", "h3"), data_table(f["beds"]),
            P("fn_hostel_occupancy_pct", "h2"), code(sql_object("04_functions.sql", "fn_hostel_occupancy_pct")),
            P("Output", "h3"), data_table(f["occupancy"]),
            P("fn_staff_open_workload", "h2"), code(sql_object("04_functions.sql", "fn_staff_open_workload")),
            P("Output (maintenance staff of Aravali Block)", "h3"), data_table(f["workload"])]


def sec_cursor():
    return [P("<b>sp_auto_assign_open_complaints</b> uses an explicit cursor (OPEN – FETCH loop – CLOSE) to process every "
              "open, unassigned complaint, High priority and oldest first. For each row it finds the least busy maintenance "
              "worker with the right specialization in the student's hostel (ordered by "
              "<font name='Mono'>fn_staff_open_workload</font>) and calls <font name='Mono'>sp_assign_complaint</font>. "
              "It is the <i>Auto-assign open complaints</i> button for the warden."),
            code(sql_object("06_cursor.sql", "sp_auto_assign_open_complaints")),
            P("Execution output (T6)", "h3"), code(demo_section("T6")),
            P("<b>sp_hostel_report</b> uses a cursor FOR loop over hostels and a <b>parameterised cursor</b> over the room "
              "types of each hostel to print an occupancy report. The application reads its DBMS_OUTPUT lines and shows them "
              "on the Reports page."),
            code(sql_object("06_cursor.sql", "sp_hostel_report")),
            P("Execution output (T7)", "h3"), code(demo_section("T7"))]


def sec_triggers():
    return [table([["Trigger", "Timing / event", "Automatic action"],
                   ["trg_allocation_validate", "BEFORE INSERT on ROOM_ALLOCATION", "Rejects a bed number larger than the room capacity."],
                   ["trg_allocation_occupancy", "AFTER INSERT / DELETE / UPDATE OF status on ROOM_ALLOCATION", "Adds or subtracts 1 from ROOM.occupied_count; the CHECK on ROOM stops overflow."],
                   ["trg_complaint_status", "BEFORE INSERT / UPDATE on COMPLAINT", "Open→Assigned when staff is set; blocks In Progress/Resolved without staff; stamps resolved_date."],
                   ["trg_visitor_pass_qr", "BEFORE INSERT / UPDATE OF status on VISITOR_PASS", "Generates a unique QR code value when a pass is approved."]],
                  [42 * mm, 56 * mm, 68 * mm]),
            P("trg_allocation_occupancy", "h2"), code(sql_object("07_triggers.sql", "trg_allocation_occupancy")),
            P("trg_allocation_validate", "h2"), code(sql_object("07_triggers.sql", "trg_allocation_validate")),
            P("trg_complaint_status", "h2"), code(sql_object("07_triggers.sql", "trg_complaint_status")),
            P("Execution output (T3: complaint lifecycle)", "h3"), code(demo_section("T3")),
            P("trg_visitor_pass_qr", "h2"), code(sql_object("07_triggers.sql", "trg_visitor_pass_qr")),
            P("Execution output (T5)", "h3"), code(demo_section("T5")),
            P("Status of all PL/SQL objects", "h2"), data_table(load("objects.json"), 20)]


def sec_crud():
    q = {e["label"]: e for e in load("queries.json")["queries"]}
    items = [P("INSERT, UPDATE and DELETE are used throughout the application (registration, requests, approvals, "
               "notices, marketplace, inventory). The statements below were executed in SQL and rolled back.")]
    for label in ("Q19", "Q20", "Q21"):
        e = q[label]
        items += [P(f"{label}. {escape(e['title'])}", "h2"), code(e["sql"])]
        for r in e["results"]:
            items.append(data_table(r))
    items += [P("Full create–update–delete cycle on an extension table (T9)", "h2"), code(demo_section("T9")),
              P("CRUD through the application", "h2"),
              table([["Operation", "Screen / endpoint", "SQL executed by Hibernate"],
                     ["Create", "Student raises a complaint — POST /api/complaints", "INSERT INTO complaint …"],
                     ["Read", "Complaints page — GET /api/complaints?status=Open", "SELECT … FROM complaint JOIN student …"],
                     ["Update", "Maintenance marks Resolved — PUT /api/complaints/{id}/status", "UPDATE complaint SET status = 'Resolved' … (trigger stamps resolved_date)"],
                     ["Delete", "Student withdraws a pending leave — DELETE /api/leaves/{id}", "DELETE FROM leave_request WHERE leave_id = ?"]],
                    [22 * mm, 80 * mm, 64 * mm])]
    return items


def sec_connectivity(short=False):
    yml = (ROOT / "backend/src/main/resources/application.yml").read_text()
    datasource = re.search(r"  # ---------- Oracle.*?(?=\n  jpa:)", yml, re.S).group(0)
    plsql = (ROOT / "backend/src/main/java/com/smarthostel/repository/PlsqlRepository.java").read_text()
    alloc = re.search(r"    /\*\* sp_allocate_room.*?\n    }\n", plsql, re.S).group(0)
    api_log = (EV / "api-workflow-test-run.txt").read_text()
    passed = api_log.count("[PASS]")
    failed = api_log.count("[FAIL]")
    sec2 = re.search(r"== 2\. Room allocation.*?(?=\n== 3)", api_log, re.S).group(0)
    items = [P("The application connects to Oracle from Java (Spring Boot) through the Oracle JDBC driver "
               "(<font name='Mono'>ojdbc11</font>) and a HikariCP connection pool. The connection settings are in "
               "<font name='Mono'>application.yml</font> and can be overridden by environment variables; no credentials are "
               "written in Java code."),
             code(datasource),
             P("Two access paths share the same connection and transaction:"),
             *bullets(["<b>Spring Data JPA</b> (Hibernate) for ordinary reads and writes of the 14 entity classes; "
                       "<font name='Mono'>ddl-auto: validate</font> checks at start-up that every entity matches its Oracle table.",
                       "<b>JdbcTemplate</b> in <font name='Mono'>PlsqlRepository</font> to call the procedures and functions, and in "
                       "<font name='Mono'>ReportRepository</font> for report SQL."]),
             P("Calling a stored procedure from Java", "h2"), code(alloc),
             P("Flow of one interactive operation:  React UI → Spring Boot → Oracle (SQL / PL/SQL) → result → Spring Boot → React UI. "
               "A rule violation inside PL/SQL (ORA-20xxx) is converted by <font name='Mono'>GlobalExceptionHandler</font> into "
               "HTTP 400 with the Oracle message, which the UI shows to the user."),
             P(f"Verification: the API test script <font name='Mono'>tools/api_workflow_test.py</font> performed {passed + failed} "
               f"checks through the REST API — {passed} passed, {failed} failed. Its room-allocation part:"),
             code(sec2),
             P("Running containers and connection health", "h2"),
             P("Output captured from the running system: both containers healthy with restart policy "
               "<font name='Mono'>unless-stopped</font>, the persistent Oracle volume, and the backend's health endpoint "
               "reporting a working connection to Oracle (<font name='Mono'>db</font>) and Redis."),
             code((EV / "infrastructure.txt").read_text())]
    if not short:
        items.append(image(TX / "04_allocation_success.png", width=150 * mm,
                           caption="Figure 3: Allocation performed from the React UI; the confirmation shows the bed chosen by sp_allocate_room"))
    return items


TRANSACTIONS = [
    ("Room allocation", "Admin allocates a waiting student to a room.",
     "STUDENT, STAFF, ROOM, HOSTEL, ROOM_ALLOCATION", "sp_allocate_room, fn_available_beds, fn_find_free_bed, trg_allocation_validate, trg_allocation_occupancy",
     "One ROOM_ALLOCATION row inserted (Active); ROOM.occupied_count +1; room row locked during the check.",
     "Allocation id and bed number returned; invalid requests rejected with ORA-20011/20013/20014/20015.", "T1",
     ("04_allocation_success.png", "Room allocation page: confirmation of the allocation made by sp_allocate_room")),
    ("Vacating a room", "Admin vacates a student's room.", "ROOM_ALLOCATION, ROOM", "sp_vacate_room, trg_allocation_occupancy",
     "Allocation status Active→Vacated, vacate_date set; occupied_count −1; history row kept.", "Bed becomes free; second vacate rejected (ORA-20021).", "T2", None),
    ("Complaint lifecycle", "Student raises a complaint; warden assigns it; maintenance resolves it.", "COMPLAINT, STAFF, ROOM_ALLOCATION, ROOM",
     "INSERT, sp_assign_complaint, fn_staff_open_workload, trg_complaint_status",
     "Complaint inserted (Open by DEFAULT) → assigned_to set (Assigned) → In Progress → Resolved, resolved_date stamped by trigger.",
     "Wrong specialist rejected (ORA-20032); starting work before assignment rejected (ORA-20050).", "T3",
     ("11_complaint_detail_resolved.png", "Student's view of the resolved complaint; the resolved time was set by trg_complaint_status")),
    ("Leave approval", "Student applies for leave; warden approves.", "LEAVE_REQUEST, STAFF", "INSERT, sp_decide_leave",
     "Status Pending→Approved, approved_by set.", "Maintenance user rejected (ORA-20042); second decision rejected (ORA-20041); approval letter generated.", "T4",
     ("14_leave_approval_letter.png", "Approval letter shown to the student after the warden's decision")),
    ("Visitor pass and gate entry", "Student requests a pass; warden approves; visitor enters and leaves.", "VISITOR_PASS",
     "INSERT, UPDATE, trg_visitor_pass_qr", "qr_code generated on approval; entry_time and exit_time recorded; status Completed.",
     "QR code shown to the student; reusing a completed pass rejected.", "T5",
     ("15_visitor_qr.png", "QR code generated by trg_visitor_pass_qr, shown to the student")),
    ("Automatic complaint assignment", "Warden assigns all open complaints at once.", "COMPLAINT, STAFF, ROOM_ALLOCATION, ROOM",
     "Cursor procedure sp_auto_assign_open_complaints (calls sp_assign_complaint and fn_staff_open_workload)",
     "Every open complaint gets the least busy matching worker.", "Open complaints reduced to 0.", "T6",
     ("19_auto_assign_after.png", "Complaints filtered by status Open after auto-assign: none remain; the message confirms 19 assigned")),
]


def sec_transactions(with_images=True):
    items = [P("For each major transaction the table states the operation, the tables involved, the SQL/PL-SQL component "
               "executed, how the database is affected and the result, followed by the actual output of "
               "<font name='Mono'>10_demo_transactions.sql</font> and, where useful, the screen from the application where the "
               "same transaction was performed (automated browser test <font name='Mono'>tools/ui_transactions_test.py</font>).")]
    for i, (name, op, tables, comp, effect, result, tag, shot) in enumerate(TRANSACTIONS, start=1):
        block = [P(f"Transaction {i}: {name}", "h2"),
                 table([["Operation", op], ["Tables", tables], ["SQL / PL-SQL", comp], ["Database effect", effect], ["Result", result]],
                       [32 * mm, 134 * mm], header=False)]
        items += [CondPageBreak(70 * mm)] + block + [Spacer(1, 4), code(demo_section(tag))]
        if with_images and shot and (TX / shot[0]).exists():
            items.append(image(TX / shot[0], width=135 * mm, caption=f"Transaction {i}: {shot[1]}"))
    return items


def sec_problems():
    entries = dev_log_entries()
    items = [P("These problems actually occurred during development. They are copied from the project's development log "
               "(<font name='Mono'>docs/development-log.md</font>), where each entry records the date, the error, the cause "
               "and the fix.")]
    for e in entries:
        rows = [["Problem", md_inline(e["problem"])]]
        if e["error"]:
            rows.append(["Error", f"<font name='Mono' size='7.5'>{escape(e['error']).replace(chr(10), '<br/>')}</font>"])
        if e["cause"]:
            rows.append(["Cause", md_inline(e["cause"])])
        if e["fix"]:
            rows.append(["Solution", md_inline(e["fix"])])
        items += [CondPageBreak(40 * mm), P(f"{e['num']}. {md_inline(e['title'])}", "h2"),
                  table(rows, [24 * mm, 142 * mm], header=False), Spacer(1, 4)]
    return items


# ---------------------------------------------------------------- REVIEW 2
def build_review2():
    sections = [
        ("intro", "Project Introduction and Problem Statement"),
        ("fr", "Functional Requirements"),
        ("design", "Final Database Design"),
        ("create", "Table Creation"),
        ("constraints", "Constraints"),
        ("population", "Database Population"),
        ("queries", "SQL Queries"),
        ("outputs", "Query Outputs"),
        ("procedures", "Procedures"),
        ("functions", "Functions"),
        ("cursor", "Cursor"),
        ("triggers", "Triggers"),
        ("crud", "CRUD Demonstrations"),
        ("connect", "Database Connectivity"),
        ("tx", "Real-World Transaction Demonstrations"),
        ("problems", "Problems Encountered and Solutions"),
        ("conclusion", "Conclusion"),
    ]
    counts = load("counts.json")["counts"]
    q = load("queries.json")["queries"]
    builders = {
        "intro": sec_introduction, "fr": sec_functional_requirements, "design": sec_final_design,
        "create": sec_table_creation, "constraints": sec_constraints, "population": sec_population,
        "queries": sec_queries_list, "outputs": sec_query_outputs, "procedures": sec_procedures,
        "functions": sec_functions, "cursor": sec_cursor, "triggers": sec_triggers, "crud": sec_crud,
        "connect": sec_connectivity, "tx": sec_transactions, "problems": sec_problems,
        "conclusion": lambda: [
            P("The database designed in Review 1 has been implemented in Oracle Database 23ai. The eight core tables were "
              f"created with named PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK, NOT NULL and DEFAULT constraints and populated with "
              f"a realistic synthetic dataset ({counts['STUDENT']} students, {counts['ROOM']} rooms, {counts['ROOM_ALLOCATION']} "
              f"allocations, {counts['COMPLAINT']} complaints, {counts['LEAVE_REQUEST']} leave requests and "
              f"{counts['VISITOR_PASS']} visitor passes)."),
            P(f"{len(q)} SQL queries answer real questions of the hostel administration. Four procedures, four functions, two "
              "cursor procedures and four triggers implement the business operations and keep the data consistent "
              "automatically; all of them are called by the application. Six real-world transactions were demonstrated, "
              "including the rejection of every invalid request."),
            P("The database is connected to a Spring Boot application with a React user interface, so every operation shown "
              "here can also be performed from the web application. Review 3 will demonstrate the integrated system end to end.")],
    }
    out = ROOT / "docs/review2/Harjot_Tanuj_Daksh_DBMS_Review2.pdf"
    out.parent.mkdir(parents=True, exist_ok=True)
    build(out, "BCSE302L – Database Systems Lab  |  Project Review 2", "Database Implementation",
          "Smart Hostel Management System – Project Review 2", cover("PROJECT REVIEW 2", "Database Implementation"),
          sections, builders)


# ---------------------------------------------------------------- FINAL REPORT
def screenshot_pairs(pairs):
    items = []
    for path, caption in pairs:
        p = SHOTS / path if (SHOTS / path).exists() else TX / path
        if p.exists():
            items.append(image(p, width=150 * mm, caption=caption))
    return items


def build_final():
    counts = load("counts.json")["counts"]
    q = load("queries.json")["queries"]
    api_log = (EV / "api-workflow-test-run.txt").read_text()
    redis_log = (EV / "redis-test-run.txt").read_text()
    ui_shots = sorted(TX.glob("*.png"))

    sections = [
        ("intro", "Introduction"), ("problem", "Problem Statement"), ("objectives", "Objectives"), ("scope", "Scope"),
        ("roles", "Users and Roles"), ("fr", "Functional Requirements"), ("nfr", "Non-Functional Requirements"),
        ("arch", "System Architecture"), ("dbarch", "Database Architecture"), ("er", "ER Diagram"),
        ("schema", "Relational Schema"), ("tables", "Table Design"), ("constraints", "Constraints"),
        ("dataset", "Dataset Description"), ("sql", "SQL Queries"), ("procedures", "Procedures"),
        ("functions", "Functions"), ("cursor", "Cursor"), ("triggers", "Triggers"),
        ("backend", "Backend Architecture"), ("frontend", "Frontend Architecture"), ("jwt", "JWT Authentication"),
        ("redis", "Redis Caching and Rate Limiting"), ("api", "API Design"), ("screens", "Major Application Screens"),
        ("tx", "Real-World Transactions"), ("connect", "Database Connectivity"), ("testing", "Testing"),
        ("problems", "Difficulties Encountered and Solutions"), ("limits", "Limitations"),
        ("future", "Future Enhancements"), ("conclusion", "Conclusion"), ("refs", "References"),
    ]

    def api_table():
        md = (ROOT / "docs/api.md").read_text().splitlines()
        rows = [["Method", "Path", "Roles"]]
        for line in md:
            m = re.match(r"\| (\w+) \| (GET|POST|PUT|DELETE) \| `([^`]+)` \| ([^|]+) \|", line)
            if m:
                rows.append([m.group(2), f"<font name='Mono' size='8'>{escape(m.group(3))}</font>", m.group(4).strip()])
        return rows

    def sql_summary():
        items = sec_queries_list()
        items += [P("Selected outputs", "h2"),
                  P("All queries were executed against the project database; the complete outputs are in Review 2 and in "
                    "<font name='Mono'>docs/evidence/db/queries.json</font>. Four representative results:")]
        qd = {e["label"]: e for e in q}
        for label in ("Q3", "Q5", "Q7", "Q12"):
            e = qd[label]
            items += [CondPageBreak(50 * mm), P(f"{label}. {escape(e['title'])}", "h3"), code(e["sql"]), data_table(e["results"][0], 8)]
        return items

    def testing():
        pa, fa = api_log.count("[PASS]"), api_log.count("[FAIL]")
        return [P("Testing was done against the real running system (Oracle + Redis in Docker, Spring Boot, React). "
                  "No mocked database or fake responses were used."),
                table([["Test", "How", "Result"],
                       ["Backend build & start-up", "mvn package; java -jar; /actuator/health", "Build successful; health UP for Oracle (db) and Redis; Hibernate schema validation passed for 14 entities."],
                       ["SQL scripts", "run-all.sh / reset-db.sh", "All scripts executed; 4 functions, 6 procedures, 4 triggers VALID."],
                       ["PL/SQL & constraints", "10_demo_transactions.sql", "Every valid operation succeeded; every invalid one raised the expected ORA-error (17 rejections)."],
                       ["SQL queries", "collect_db_evidence.py", f"{len(q)} queries executed without errors."],
                       ["REST API & roles", "tools/api_workflow_test.py", f"{pa} passed, {fa} failed (auth, 401/403, allocation, complaint, leave, visitor, cursor procedures, validation)."],
                       ["Redis cache & rate limit", "tools/redis-demo-test.sh", "Cache miss → hit → evict → fresh read; 5 failures → 429 → key expired → login 200."],
                       ["All screens", "tools/capture_screenshots.py (Playwright)", "Login page plus 33 screens of the 3 roles loaded with no console errors or failed API calls."],
                       ["UI transactions", "tools/ui_transactions_test.py (Playwright)", f"9 transactions performed by clicking through the UI; {len(ui_shots)} result screenshots captured."]],
                      [36 * mm, 50 * mm, 80 * mm]),
                P("API test output", "h2"), code(api_log),
                P("Redis test output", "h2"), code(redis_log)]

    builders = {
        "intro": lambda: [
            P("The Smart Hostel Management System (SHMS) is a web application that brings hostel administration into one place: "
              "room allocation, maintenance complaints, leave, visitor passes, fees, notices, mess menu, marketplace, lost &amp; "
              "found and inventory. It is a single integrated project for the Database Systems and Software Engineering laboratories."),
            P("The system consists of a React front end, a Spring Boot REST API and an Oracle database that holds all data and the "
              "core business logic in SQL and PL/SQL. Redis is used as a supporting component for caching and login rate limiting. "
              "This report describes the complete system as built and tested.")],
        "problem": lambda: sec_introduction()[:1] + [
            P("A single database with enforced relationships solves these problems: every bed, request and approval is linked to the "
              "student and staff member involved, rules are checked by the database itself, and reports can be generated at any time.")],
        "objectives": lambda: bullets([
            "Maintain a central, consistent database of hostels, rooms, students and staff.",
            "Allocate beds safely (capacity, gender and one-active-room rules) and show live vacancy.",
            "Track complaints from registration to resolution with the responsible maintenance worker.",
            "Digitise leave and visitor approvals, with an approval letter and QR-based gate entry.",
            "Provide role-based web screens for students, wardens/administrators and maintenance staff.",
            "Produce reports and a dashboard directly from the database.",
            "Demonstrate SQL and PL/SQL (procedures, functions, cursors, triggers) inside real application transactions."]),
        "scope": lambda: [P("Included in this version:"), *bullets([
            "Student, admin/warden and maintenance modules with JWT login and role-based access.",
            "Room allocation and vacancy, complaints with photos and scheduling, leave with approval letter, visitor passes with QR codes and gate check-in/out.",
            "Fee status and payment recording, notice board, mess menu, inventory, marketplace, lost &amp; found, emergency contacts.",
            "Dashboards and reports, including a report produced by a PL/SQL cursor procedure.",
            "Redis caching of slow-changing data and a login rate limit."]),
            P("Not included (see Limitations): online payment gateway, e-mail notifications, mobile apps, laundry, events and feedback modules.")],
        "roles": lambda: sec_functional_requirements()[-1:],
        "fr": lambda: sec_functional_requirements()[:2],
        "nfr": lambda: [table([["Category", "How it is met"],
                               ["Security", "BCrypt password hashes; signed JWT; role checks on every endpoint; students can only read their own data; login rate limit."],
                               ["Data integrity", "PK/FK/UNIQUE/CHECK/NOT NULL/DEFAULT; function-based unique indexes; occupancy maintained by trigger; transactions roll back on any error."],
                               ["Reliability", "Allocation locks the room row (FOR UPDATE) so two requests cannot fill the same bed; Redis failure falls back to Oracle."],
                               ["Performance", "Indexes on foreign keys and status columns; Redis cache for frequently read, rarely changed lists; one-query room lookups for lists."],
                               ["Usability", "Separate, simple screens per role; clear error messages from the database rules."],
                               ["Maintainability", "Conventional Spring Boot layers (controller/service/repository); SQL scripts as the single source of the schema."],
                               ["Portability", "Oracle and Redis run in Docker with one command; configuration via environment variables."]],
                              [32 * mm, 134 * mm])],
        "arch": lambda: [image(DIAG / "architecture.png", caption="Figure: System architecture"),
                         P("<b>Presentation layer</b> — React single-page application (Vite, React Router, Axios). <b>Application layer</b> — "
                           "Spring Boot REST API with Spring Security (JWT), Spring Data JPA, JdbcTemplate and Spring Cache. <b>Data layer</b> — "
                           "Oracle Database 23ai Free (primary database) and Redis 7 (cache and temporary counters), both in Docker containers "
                           "with health checks and restart policies. Oracle uses a persistent Docker volume; Redis persistence is disabled because it holds only rebuildable data."),
                         image(DIAG / "allocation_sequence.png", caption="Figure: Sequence of a room allocation through all layers")],
        "dbarch": lambda: [P("Oracle Database 23ai Free runs in the container <font name='Mono'>shms-oracle</font> "
                             "(image <font name='Mono'>gvenzl/oracle-free:23-slim</font>). The application schema is the user "
                             "<font name='Mono'>user1</font> in the pluggable database <font name='Mono'>FREEPDB1</font>. Data files are "
                             "stored in the named volume <font name='Mono'>shms-oracle-data</font>, which survived container removal in testing."),
                           table([["Layer", "Objects"],
                                  ["Core tables (DBMS design)", "HOSTEL, ROOM, STUDENT, STAFF, ROOM_ALLOCATION, COMPLAINT, LEAVE_REQUEST, VISITOR_PASS"],
                                  ["Extension tables (application)", "FEE_PAYMENT, ANNOUNCEMENT, MARKETPLACE_LISTING, INVENTORY_ITEM, LOST_FOUND_ITEM, MESS_MENU"],
                                  ["Integrity", "Named PK/FK/UNIQUE/CHECK constraints, defaults, 2 function-based unique indexes, FK indexes"],
                                  ["PL/SQL", "4 functions, 4 procedures, 2 cursor procedures, 4 triggers"],
                                  ["Scripts", "01–03 schema, 04–07 PL/SQL, 08 seed data, 09 queries, 10 demo transactions (run-all.sh, reset-db.sh)"]],
                                 [50 * mm, 116 * mm]),
                           P("Redis (container <font name='Mono'>shms-redis</font>) is an application infrastructure component, not part of "
                             "the relational design: it stores no business records and appears in no ER diagram.")],
        "er": lambda: [image(DIAG / "er_core.png", caption="Figure: ER diagram of the core schema", max_h=170 * mm),
                       image(DIAG / "er_extension.png", caption="Figure: Extension tables", max_h=40 * mm)],
        "schema": lambda: sec_final_design()[:2] + sec_final_design()[4:6] + sec_final_design()[-2:],
        "tables": lambda: [P("Column definitions of the core tables (complete script in the appendix of Review 2 and in "
                             "<font name='Mono'>database/schema/01_core_schema.sql</font>):"),
                           code((DB / "sql_files/01_core_schema.sql").read_text()),
                           P("Extension tables", "h2"), code((DB / "sql_files/03_extension_tables.sql").read_text(), max_lines=70)],
        "constraints": lambda: sec_constraints()[:5] + sec_constraints()[9:12],
        "dataset": lambda: sec_population()[:4],
        "sql": sql_summary,
        "procedures": sec_procedures, "functions": sec_functions, "cursor": sec_cursor, "triggers": sec_triggers,
        "backend": lambda: [P("The backend is a conventional Spring Boot 3.5 application (Java 17, Maven) with one package per responsibility:"),
                            table([["Package", "Contents"],
                                   ["controller", "18 REST controllers, one per module; validate input and check roles only"],
                                   ["service", "19 services with the business logic and @Transactional boundaries; cache annotations"],
                                   ["repository", "14 Spring Data JPA repositories, PlsqlRepository (procedures/functions), ReportRepository (report SQL)"],
                                   ["model", "14 JPA entities mapped to the Oracle tables; trigger-maintained columns marked @Generated"],
                                   ["dto", "Request/response records; entities and password hashes never leave the API"],
                                   ["security", "JwtService, JwtAuthenticationFilter, SecurityConfig, CustomUserDetailsService, AppUserDetails, LoginRateLimiter"],
                                   ["config", "RedisCacheConfig (TTL per cache, JSON values, fallback), WebConfig (CORS, uploads), CacheNames"],
                                   ["exception", "GlobalExceptionHandler — converts errors (incl. ORA-20xxx) into JSON with the right HTTP status"],
                                   ["util", "OracleErrorUtil — reads the Oracle error code and message"]],
                                  [28 * mm, 138 * mm]),
                            P("Hibernate runs with <font name='Mono'>ddl-auto: validate</font>: the SQL scripts create the schema and Hibernate "
                              "only verifies it. Business operations that must be atomic call PL/SQL inside a service transaction.")],
        "frontend": lambda: [P("The React application (Vite) has three role-specific areas with a shared layout and design system "
                               "(warm neutral palette, serif headings, restrained components):"),
                             table([["Area", "Screens"],
                                    ["Student", "Dashboard, My Room, Notice Board, Complaints, Leave (+ approval letter), Visitor Passes (+ QR), Fees, Mess Menu, Marketplace, Lost & Found, Emergency Contacts"],
                                    ["Admin / Warden", "Dashboard (charts), Reports, Students, Room Allocation, Rooms & Vacancy, Hostels, Complaints (+ assign / auto-assign), Leave Approvals, Visitors (+ gate check), Fees, Staff, Notice Board, Mess Menu, Inventory, Marketplace, Lost & Found, Emergency Contacts"],
                                    ["Maintenance", "Dashboard, My Jobs (status, schedule, completion photo), Inventory, Notice Board, Contacts"]],
                                   [30 * mm, 136 * mm]),
                             *bullets(["<b>Routing</b>: React Router; <font name='Mono'>ProtectedRoute</font> only allows the permitted roles.",
                                       "<b>API calls</b>: one Axios client adds the JWT and redirects to login on 401.",
                                       "<b>State</b>: <font name='Mono'>useApi</font> hook with loading/error states; stale responses are ignored.",
                                       "<b>Feedback</b>: toast messages; database rule violations are shown with the Oracle message."])],
        "jwt": lambda: [*bullets([
            "Login: <font name='Mono'>POST /api/auth/login</font> → rate-limit check (Redis) → user loaded from STAFF (e-mail) or STUDENT (registration number) → BCrypt password check.",
            "Token: HS256-signed JWT containing username, user id, role, name and an 8-hour expiry; the secret comes from configuration.",
            "Requests: <font name='Mono'>JwtAuthenticationFilter</font> verifies the signature and expiry on every request and sets the user; no server session is kept.",
            "Authorisation: <font name='Mono'>@PreAuthorize</font> on each endpoint (STUDENT / ADMIN / WARDEN / MAINTENANCE); services use the user id from the token for personal data.",
            "Errors: missing or invalid token → 401; wrong role → 403 (both verified by the API test)."]),
            code(re.search(r"    @Override.*", (ROOT / "backend/src/main/java/com/smarthostel/security/JwtAuthenticationFilter.java").read_text(), re.S).group(0))],
        "redis": lambda: [
            P("Redis is used for two purposes only; Oracle remains the source of truth for every record."),
            P("Caching", "h2"),
            table([["Cache key", "TTL", "Evicted when"],
                   ["shms:hostels::all", "60 min", "hostel created / updated"],
                   ["shms:announcements::&lt;hostel&gt;", "10 min", "notice created / edited / deleted"],
                   ["shms:messMenu::&lt;hostel&gt;", "6 h", "a meal is edited"],
                   ["shms:emergencyContacts::&lt;hostel&gt;", "60 min", "staff created / edited / deleted"],
                   ["shms:dashboardStats::admin", "60 s", "room allocated / vacated"]], [70 * mm, 22 * mm, 74 * mm]),
            P("Allocations, occupancy, complaint/leave/visitor status and fees are never cached. Caching uses Spring's "
              "<font name='Mono'>@Cacheable</font> and <font name='Mono'>@CacheEvict</font>; if Redis is unavailable the error is logged and data is read from Oracle."),
            P("Login rate limiting", "h2"),
            P("Failed logins are counted per IP address in the key <font name='Mono'>rate:login:fail:&lt;ip&gt;</font> "
              "(INCR, 300-second EXPIRE on the first failure). From the sixth attempt within the window the API returns HTTP 429 with a "
              "Retry-After header, even for a correct password; the key expires automatically. Test output:"),
            code(redis_log),
            image(TX / "20_login_rate_limited.png", width=130 * mm, caption="Figure: Login page after five failed attempts (HTTP 429)")],
        "api": lambda: [P("REST endpoints (generated from the controller source). All except login/register need a JWT; errors return "
                          "JSON with status 400, 401, 403, 404, 409 or 429."),
                        table(api_table(), [18 * mm, 100 * mm, 48 * mm])],
        "screens": lambda: screenshot_pairs([
            ("01_login.png", "Login page"), ("student_student.png", "Student dashboard"),
            ("student_student_room.png", "Student: My Room"), ("student_student_complaints.png", "Student: complaints"),
            ("student_student_mess.png", "Mess menu"), ("admin_admin.png", "Admin dashboard"),
            ("admin_admin_allocations.png", "Room allocation"), ("admin_admin_rooms.png", "Rooms and vacancy"),
            ("admin_admin_complaints.png", "Complaint management"), ("07_complaint_assign_modal.png", "Assigning a complaint (workload from fn_staff_open_workload)"),
            ("admin_admin_leaves.png", "Leave approvals"), ("admin_admin_visitors.png", "Visitors and gate check"),
            ("admin_admin_fees.png", "Fees"), ("admin_admin_reports.png", "Reports"),
            ("maintenance_maintenance.png", "Maintenance dashboard"), ("09_maintenance_resolve_form.png", "Maintenance resolving a complaint"),
            ("student_student_marketplace.png", "Marketplace")]),
        "tx": lambda: sec_transactions(True),
        "connect": lambda: sec_connectivity(short=True),
        "testing": testing,
        "problems": sec_problems,
        "limits": lambda: bullets([
            "No online payment gateway; payments are recorded by the office.",
            "No e-mail notifications; status changes appear on the student dashboard.",
            "Wardens can view all hostels, not only their own.",
            "The login rate limit is per IP address; users behind one network address share a counter.",
            "Uploaded images are stored on the backend server's disk, not in the database or an object store.",
            "The data is synthetic; real deployment would import institutional records.",
            "Laundry, events and feedback modules from the SE scope are not implemented in this version."]),
        "future": lambda: bullets([
            "Online fee payment through a payment gateway.", "E-mail / push notifications for status changes.",
            "Mobile application.", "Per-hostel access control for wardens.",
            "Laundry, event registration and feedback modules.", "Predictive maintenance using complaint history.",
            "Integration with the college ERP for student records."]),
        "conclusion": lambda: [
            P("The Smart Hostel Management System was built as one integrated application: a React interface, a Spring Boot API "
              "with JWT security and Redis support, and an Oracle database that enforces the hostel's rules. The Review 1 design was "
              f"implemented as eight core tables with full constraints and populated with realistic synthetic data ({counts['STUDENT']} "
              f"students, {counts['COMPLAINT']} complaints and more). {len(q)} SQL queries, four procedures, four functions, two cursor "
              "procedures and four triggers are used directly by the application's workflows."),
            P("Every major transaction — room allocation, vacating, the complaint lifecycle, leave approval, visitor passes and "
              "automatic assignment — was demonstrated in SQL and performed through the web interface, and all outputs and "
              "screenshots in this report come from the running system. The difficulties met during development, such as the "
              "time-zone mismatch between the database container and the application, were recorded and solved.")],
        "refs": lambda: [P(t) for t in [
            "1. A. Silberschatz, H. F. Korth, S. Sudarshan, <i>Database System Concepts</i>, 7th ed., McGraw-Hill, 2019.",
            "2. Oracle Database 23ai SQL Language Reference and PL/SQL Language Reference — docs.oracle.com",
            "3. gvenzl/oracle-free Docker image — github.com/gvenzl/oci-oracle-free",
            "4. Spring Boot Reference Documentation — docs.spring.io/spring-boot",
            "5. Spring Security and Spring Data JPA / Redis Reference Documentation — docs.spring.io",
            "6. JJWT (Java JWT) — github.com/jwtk/jjwt",
            "7. Redis Documentation — redis.io/docs",
            "8. React Documentation — react.dev; Vite — vite.dev",
            "9. R. S. Pressman, <i>Software Engineering: A Practitioner's Approach</i>, 8th ed., McGraw-Hill, 2014.",
            "10. Login-page photograph by Jonathan Borba, Unsplash (Unsplash License)."]],
    }
    out = ROOT / "docs/final/Harjot_Tanuj_Daksh_DBMS_Final_Project.pdf"
    out.parent.mkdir(parents=True, exist_ok=True)
    build(out, "BCSE302L – Database Systems Lab  |  Final Project Report", "Integrated System – Final Report",
          "Smart Hostel Management System – Final Project Report", cover("FINAL PROJECT REPORT", "Integrated System and Final Demonstration"),
          sections, builders)


if __name__ == "__main__":
    which = sys.argv[1] if len(sys.argv) > 1 else "all"
    if which in ("review2", "all"):
        build_review2()
    if which in ("final", "all"):
        build_final()
