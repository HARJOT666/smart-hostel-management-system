"""
Collects REAL outputs from the Oracle database for the reports and saves them in docs/evidence/db/:
  counts.json       row count of every table
  constraints.json  constraints per table and type
  samples.json      first rows of every table
  objects.json      PL/SQL objects and their status
  queries.json      every query of database/queries/09_queries.sql with its real result
  demo_transactions.txt  output of database/transactions/10_demo_transactions.sql (SQL*Plus)
  sql_files/*.sql   copies of the scripts (for code listings)

Run on a freshly seeded database:  database/reset-db.sh && .venv/bin/python tools/collect_db_evidence.py
"""
import datetime as dt
import decimal
import json
import re
import subprocess
from pathlib import Path

import oracledb

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs" / "evidence" / "db"
CORE = ["HOSTEL", "ROOM", "STUDENT", "STAFF", "ROOM_ALLOCATION", "COMPLAINT", "LEAVE_REQUEST", "VISITOR_PASS"]
EXT = ["FEE_PAYMENT", "ANNOUNCEMENT", "MARKETPLACE_LISTING", "INVENTORY_ITEM", "LOST_FOUND_ITEM", "MESS_MENU"]


def clean(v):
    if isinstance(v, (dt.datetime, dt.date)):
        return v.strftime("%d-%m-%Y %H:%M") if isinstance(v, dt.datetime) and (v.hour or v.minute) else v.strftime("%d-%m-%Y")
    if isinstance(v, decimal.Decimal):
        return float(v) if v % 1 else int(v)
    if isinstance(v, float) and v.is_integer():
        return int(v)
    return v


def rows_of(cur, limit=None):
    cols = [d[0] for d in cur.description]
    data = cur.fetchmany(limit) if limit else cur.fetchall()
    return {"columns": cols, "rows": [[clean(v) for v in r] for r in data]}


def split_queries(text):
    """Yields (label, title, [statements]) for every 'PROMPT Qn. title' block."""
    blocks = re.split(r"^PROMPT (Q\d+)\. (.*)$", text, flags=re.M)
    for i in range(1, len(blocks), 3):
        body = blocks[i + 2]
        body = re.split(r"^(PROMPT|ROLLBACK)", body, flags=re.M)[0]
        stmts = [s.strip() for s in body.split(";") if s.strip() and not s.strip().startswith("--")]
        yield blocks[i], blocks[i + 1].strip(), stmts


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    env = dict(l.split("=", 1) for l in (ROOT / "database/.env").read_text().split() if "=" in l)
    con = oracledb.connect(user=env["DB_USERNAME"], password=env["DB_PASSWORD"], dsn="localhost:1521/FREEPDB1")
    cur = con.cursor()
    stamp = dt.datetime.now().strftime("%d %b %Y, %H:%M")

    counts = {}
    for t in CORE + EXT:
        cur.execute(f"SELECT COUNT(*) FROM {t}")
        counts[t] = cur.fetchone()[0]
    (OUT / "counts.json").write_text(json.dumps({"captured": stamp, "counts": counts}, indent=1))

    cur.execute("""
        SELECT table_name,
               SUM(CASE WHEN constraint_type = 'P' THEN 1 ELSE 0 END) pk,
               SUM(CASE WHEN constraint_type = 'R' THEN 1 ELSE 0 END) fk,
               SUM(CASE WHEN constraint_type = 'U' THEN 1 ELSE 0 END) uq,
               SUM(CASE WHEN constraint_type = 'C' AND search_condition_vc NOT LIKE '%IS NOT NULL' THEN 1 ELSE 0 END) ck,
               SUM(CASE WHEN constraint_type = 'C' AND search_condition_vc LIKE '%IS NOT NULL' THEN 1 ELSE 0 END) nn
          FROM user_constraints GROUP BY table_name""")
    cons = {r[0]: dict(zip(["PK", "FK", "UNIQUE", "CHECK", "NOT NULL"], r[1:])) for r in cur}
    cur.execute("SELECT table_name, COUNT(*) FROM user_tab_columns WHERE data_default IS NOT NULL GROUP BY table_name")
    for t, n in cur:
        cons.setdefault(t, {})["DEFAULT"] = n
    cur.execute("""SELECT index_name, table_name FROM user_indexes WHERE index_type LIKE 'FUNCTION-BASED%' AND uniqueness = 'UNIQUE'""")
    fbi = [list(r) for r in cur]
    cur.execute("""SELECT constraint_name, table_name, search_condition_vc FROM user_constraints
                    WHERE constraint_type = 'C' AND constraint_name LIKE 'CK%' ORDER BY table_name, constraint_name""")
    checks = [list(r) for r in cur]
    cur.execute("""SELECT c.constraint_name, c.table_name, cc.column_name, r.table_name
                     FROM user_constraints c
                     JOIN user_cons_columns cc ON cc.constraint_name = c.constraint_name
                     JOIN user_constraints r ON r.constraint_name = c.r_constraint_name
                    WHERE c.constraint_type = 'R' ORDER BY c.table_name""")
    fks = [list(r) for r in cur]
    (OUT / "constraints.json").write_text(json.dumps({"per_table": cons, "function_based_unique": fbi,
                                                      "checks": checks, "foreign_keys": fks}, indent=1))

    samples = {}
    order = {"STUDENT": "student_id", "STAFF": "staff_id", "ROOM": "room_id"}
    for t in CORE + EXT:
        cols = "*" if t not in ("STUDENT", "STAFF") else \
            ("student_id, registration_no, name, email, phone, gender, year, branch, cgpa, SUBSTR(password_hash,1,12)||'...' AS password_hash"
             if t == "STUDENT" else "staff_id, name, role, specialization, phone, email, hostel_id, SUBSTR(password_hash,1,12)||'...' AS password_hash")
        cur.execute(f"SELECT {cols} FROM {t} ORDER BY 1 FETCH FIRST 5 ROWS ONLY")
        samples[t] = rows_of(cur)
    (OUT / "samples.json").write_text(json.dumps(samples, indent=1))

    cur.execute("SELECT object_name, object_type, status FROM user_objects WHERE object_type IN ('FUNCTION','PROCEDURE','TRIGGER') ORDER BY object_type, object_name")
    (OUT / "objects.json").write_text(json.dumps(rows_of(cur), indent=1))

    queries = []
    for label, title, stmts in split_queries((ROOT / "database/queries/09_queries.sql").read_text()):
        entry = {"label": label, "title": title, "sql": ";\n".join(stmts) + ";", "results": []}
        for s in stmts:
            cur.execute(s)
            if cur.description:
                entry["results"].append(rows_of(cur, 12))
            else:
                entry["results"].append({"message": f"{cur.rowcount} row(s) affected"})
        queries.append(entry)
    con.rollback()
    (OUT / "queries.json").write_text(json.dumps({"captured": stamp, "queries": queries}, indent=1))

    # functions called directly
    cur.execute("""SELECT h.hostel_name, fn_hostel_occupancy_pct(h.hostel_id) AS occupancy_pct FROM hostel h ORDER BY h.hostel_id""")
    fn1 = rows_of(cur)
    cur.execute("""SELECT room_number, capacity, occupied_count, fn_available_beds(room_id) AS available_beds, fn_find_free_bed(room_id) AS next_free_bed
                     FROM room WHERE hostel_id = 1 ORDER BY room_id FETCH FIRST 6 ROWS ONLY""")
    fn2 = rows_of(cur)
    cur.execute("""SELECT staff_id, name, specialization, fn_staff_open_workload(staff_id) AS open_jobs FROM staff
                    WHERE role = 'Maintenance' AND hostel_id = 1 ORDER BY staff_id""")
    fn3 = rows_of(cur)
    (OUT / "functions.json").write_text(json.dumps({"occupancy": fn1, "beds": fn2, "workload": fn3}, indent=1))
    con.close()

    demo = subprocess.run([str(ROOT / "database/run-sql.sh"), "transactions/10_demo_transactions.sql"],
                          capture_output=True, text=True).stdout
    demo = "\n".join(l.replace("\t", "    ") for l in demo.splitlines() if l.strip())
    (OUT / "demo_transactions.txt").write_text(f"-- captured {stamp}\n" + demo + "\n")

    sql_dir = OUT / "sql_files"
    sql_dir.mkdir(exist_ok=True)
    for f in ["schema/01_core_schema.sql", "schema/02_core_constraints.sql", "schema/03_extension_tables.sql",
              "functions/04_functions.sql", "procedures/05_procedures.sql", "cursor/06_cursor.sql",
              "triggers/07_triggers.sql"]:
        (sql_dir / Path(f).name).write_text((ROOT / "database" / f).read_text())
    print("evidence written to", OUT, "| captured", stamp)
    print("counts:", counts)


if __name__ == "__main__":
    main()
