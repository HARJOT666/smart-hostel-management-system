"""
Performs the main hostel transactions THROUGH THE REACT UI (headless Chromium),
exactly as a user would click them, and saves a screenshot after each step to
docs/evidence/transactions/. Data really changes in Oracle.

Needs backend (:8080) + frontend (:5173) running. Reset afterwards with database/reset-db.sh.
Run: .venv/bin/python tools/ui_transactions_test.py
"""
import re
import subprocess
from datetime import date
from pathlib import Path

from playwright.sync_api import expect, sync_playwright

BASE = "http://localhost:5173"
ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs" / "evidence" / "transactions"
PASSWORD = "Hostel@123"
NEW_REG = "26BYB1999"
step_no = 0


def shot(page, name):
    global step_no
    step_no += 1
    page.wait_for_timeout(500)
    path = OUT / f"{step_no:02d}_{name}.png"
    page.screenshot(path=str(path))
    print(f"  screenshot {path.name}")


def field(page, label):
    """Input/select/textarea inside the open modal, found by its visible label."""
    return page.locator(f'.modal .field:has(> label:text-is("{label}"))').locator("input, select, textarea").first


def toast_text(page):
    t = page.locator(".toast").last
    t.wait_for()
    return t.inner_text()


def login(browser, username, password=PASSWORD):
    ctx = browser.new_context(viewport={"width": 1440, "height": 900})
    page = ctx.new_page()
    page.goto(BASE + "/login")
    page.fill("#username", username)
    page.fill("#password", password)
    page.click(".auth-submit")
    page.wait_for_url(re.compile(r".*/(student|admin|maintenance)$"))
    return ctx, page


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for old in OUT.glob("*.png"):
        old.unlink()
    with sync_playwright() as p:
        browser = p.chromium.launch()

        print("T1 Student registration")
        ctx = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx.new_page()
        page.goto(BASE + "/register")
        page.locator(".field:has(> label:text-is('Registration no.')) input").fill(NEW_REG)
        page.locator(".field:has(> label:text-is('Full name')) input").fill("Demo Resident")
        page.locator(".field:has(> label:text-is('E-mail')) input").fill("demo.resident@student.shms.local")
        page.locator(".field:has(> label:text-is('Phone')) input").fill("9000012345")
        page.locator(".field:has(> label:text-is('Branch')) select").select_option("CSE (Cyber Security)")
        page.locator(".field:has(> label:text-is('Password')) input").fill("Resident@2026")
        shot(page, "register_form")
        page.click(".auth-submit")
        page.wait_for_url("**/login")
        expect(page.locator(".alert-success")).to_be_visible()
        shot(page, "register_success")
        ctx.close()

        print("T2 Room allocation (sp_allocate_room)")
        ctx, admin = login(browser, "admin@shms.local")
        admin.goto(BASE + "/admin/allocations")
        admin.locator("tbody tr").first.wait_for()          # wait until the waiting list has loaded
        row = admin.locator("tr", has_text=NEW_REG)
        while not row.count():                      # page through the waiting list
            admin.get_by_role("button", name="Next").first.click()
        row.get_by_role("button", name="Select").click()
        admin.wait_for_load_state("networkidle")
        shot(admin, "allocation_choose_room")
        admin.locator(".card:has(h2:text('2. Choose a room')) tbody tr").first.get_by_role("button", name="Allocate").click()
        expect(admin.locator(".alert-success")).to_be_visible()
        shot(admin, "allocation_success")

        print("T3 Complaint raised with photo")
        sctx, student = login(browser, "24BYB1001")
        student.goto(BASE + "/student/complaints")
        student.get_by_role("button", name="New complaint").click()
        field(student, "Category").select_option("Plumbing")
        field(student, "Priority").select_option("High")
        field(student, "Describe the problem").fill("Bathroom tap keeps leaking and the floor stays wet.")
        student.locator('.modal input[type="file"]').set_input_files(str(ROOT / "tools" / "sample-photo.jpg"))
        shot(student, "complaint_form")
        student.get_by_role("button", name="Submit complaint").click()
        complaint_id = re.search(r"#(\d+)", toast_text(student)).group(1)
        print(f"  complaint id {complaint_id}")
        student.wait_for_load_state("networkidle")
        shot(student, "complaint_raised")

        print("T4 Complaint assigned (sp_assign_complaint)")
        admin.goto(BASE + "/admin/complaints")
        admin.locator("tr", has=admin.locator(f"td:text-is('{complaint_id}')")).get_by_role("button", name="Assign").click()
        admin.wait_for_load_state("networkidle")
        shot(admin, "complaint_assign_modal")
        admin.locator(".modal").get_by_role("button", name="Assign").click()
        toast_text(admin)
        admin.locator(".toolbar select").first.select_option("Assigned")
        admin.wait_for_load_state("networkidle")
        shot(admin, "complaint_assigned")

        print("T5 Maintenance resolves the complaint (trigger stamps resolved date)")
        # find the plumber's e-mail through the API-backed staff page is not needed: log in as the assigned staff
        assigned_email = subprocess.run(
            ["docker", "exec", "-i", "shms-oracle", "sqlplus", "-s", "user1/user1Password@localhost/FREEPDB1"],
            input=f"SET HEADING OFF FEEDBACK OFF\nSELECT s.email FROM complaint c JOIN staff s ON s.staff_id = c.assigned_to WHERE c.complaint_id = {complaint_id};\n",
            capture_output=True, text=True).stdout.strip()
        print(f"  assigned to {assigned_email}")
        mctx, maint = login(browser, assigned_email)
        maint.goto(BASE + "/maintenance/jobs")
        job = maint.locator("tr", has=maint.locator(f"td:text-is('{complaint_id}')"))
        job.get_by_role("button", name="Update").click()
        field(maint, "Remarks").fill("Inspected the tap; washer needs replacement.")
        maint.get_by_role("button", name="Save").click()
        toast_text(maint)
        job.get_by_role("button", name="Update").click()
        field(maint, "Status").select_option("Resolved")
        field(maint, "Remarks").fill("Replaced the washer and tested. No more leakage.")
        maint.locator('.modal input[type="file"]').set_input_files(str(ROOT / "tools" / "sample-photo.jpg"))
        shot(maint, "maintenance_resolve_form")
        maint.get_by_role("button", name="Save").click()
        toast_text(maint)
        maint.wait_for_load_state("networkidle")
        shot(maint, "maintenance_resolved")
        student.reload()
        student.wait_for_load_state("networkidle")
        student.locator("tr", has=student.locator(f"td:text-is('{complaint_id}')")).get_by_role("button", name="Details").click()
        shot(student, "complaint_detail_resolved")
        student.keyboard.press("Escape")
        student.locator(".modal-close").click()
        mctx.close()

        print("T6 Leave applied -> approved (sp_decide_leave) -> approval letter")
        student.goto(BASE + "/student/leaves")
        student.get_by_role("button", name="Apply for leave").click()
        field(student, "From").fill("2026-11-06")
        field(student, "To").fill("2026-11-09")
        field(student, "Reason").fill("Elder brother's engagement ceremony")
        field(student, "Destination").fill("Jaipur")
        student.get_by_role("button", name="Submit request").click()
        toast_text(student)
        shot(student, "leave_applied")
        wctx, warden = login(browser, "warden@shms.local")
        warden.goto(BASE + "/admin/leaves")
        warden.wait_for_load_state("networkidle")
        shot(warden, "leave_pending_list")
        warden.locator("tr", has_text="Elder brother's engagement ceremony").get_by_role("button", name="Approve").click()
        toast_text(warden)
        student.reload()
        student.wait_for_load_state("networkidle")
        student.locator("tr", has_text="Elder brother's engagement ceremony").get_by_role("link", name="Approval letter").click()
        student.wait_for_load_state("networkidle")
        shot(student, "leave_approval_letter")

        print("T7 Visitor pass -> approved (QR trigger) -> gate entry and exit")
        student.goto(BASE + "/student/visitors")
        student.get_by_role("button", name="Request a pass").click()
        field(student, "Visitor name").fill("Rakesh Mehta")
        field(student, "Visitor phone").fill("9876500011")
        field(student, "Visit date").fill(date.today().isoformat())
        field(student, "Purpose").fill("Bringing semester books from home")
        student.get_by_role("button", name="Send request").click()
        toast_text(student)
        warden.goto(BASE + "/admin/visitors")
        warden.fill(".toolbar .search", "Rakesh Mehta")          # search like a warden would
        warden.wait_for_load_state("networkidle")
        warden.locator("tr", has_text="Bringing semester books from home").get_by_role("button", name="Approve").click()
        qr = re.search(r"QR (\S+)", toast_text(warden)).group(1)
        print(f"  QR {qr}")
        student.reload()
        student.wait_for_load_state("networkidle")
        student.locator("tr", has_text="Bringing semester books from home").get_by_role("button", name="Show QR").click()
        shot(student, "visitor_qr")
        for label in ("entry", "exit"):
            warden.fill(".card input.mono", qr)
            warden.get_by_role("button", name="Check in / out").click()
            expect(warden.locator(".alert-success")).to_be_visible()
            shot(warden, f"visitor_gate_{label}")

        print("T8 Auto-assign (cursor procedure)")
        admin.goto(BASE + "/admin/complaints")
        admin.wait_for_load_state("networkidle")
        shot(admin, "auto_assign_before")
        admin.get_by_role("button", name="Auto-assign open complaints").click()
        print("  " + toast_text(admin))
        admin.wait_for_load_state("networkidle")
        shot(admin, "auto_assign_after")

        print("T9 Login rate limit (Redis) on the login page")
        ctx2 = browser.new_context(viewport={"width": 1440, "height": 900})
        page = ctx2.new_page()
        page.goto(BASE + "/login")
        for i in range(6):
            page.fill("#username", "warden@shms.local")
            page.fill("#password", f"wrong-{i}")
            page.click(".auth-submit")
            page.locator(".alert-error").wait_for()
            page.wait_for_timeout(300)
        print("  " + page.locator(".alert-error").inner_text())
        shot(page, "login_rate_limited")
        browser.close()

    # clear the limiter key so the next demo can log in immediately
    for key in subprocess.run(["docker", "exec", "shms-redis", "redis-cli", "--scan", "--pattern", "rate:login:fail:*"],
                              capture_output=True, text=True).stdout.split():
        subprocess.run(["docker", "exec", "shms-redis", "redis-cli", "DEL", key], capture_output=True)
    print("\nAll UI transactions completed.")


if __name__ == "__main__":
    main()
