"""
Opens the REAL running application in headless Chromium, signs in through the login
page for each role, visits every screen and saves screenshots to docs/evidence/screenshots/.
It also reports browser console errors and failed API calls (HTTP >= 400).

Needs: backend on :8080, frontend dev server on :5173.
Run:   .venv/bin/python tools/capture_screenshots.py
"""
import sys
from pathlib import Path

from playwright.sync_api import sync_playwright

BASE = "http://localhost:5173"
OUT = Path(__file__).resolve().parent.parent / "docs" / "evidence" / "screenshots"
PASSWORD = "Hostel@123"

ROLES = {
    "student": ("24BYB1001", ["/student", "/student/room", "/student/complaints", "/student/leaves",
                              "/student/visitors", "/student/fees", "/student/notices", "/student/mess",
                              "/student/marketplace", "/student/lost-found", "/student/contacts"]),
    "admin": ("admin@shms.local", ["/admin", "/admin/students", "/admin/allocations", "/admin/rooms",
                                   "/admin/hostels", "/admin/complaints", "/admin/leaves", "/admin/visitors",
                                   "/admin/fees", "/admin/staff", "/admin/notices", "/admin/mess",
                                   "/admin/inventory", "/admin/marketplace", "/admin/lost-found",
                                   "/admin/contacts", "/admin/reports"]),
    "maintenance": ("maint@shms.local", ["/maintenance", "/maintenance/jobs", "/maintenance/inventory",
                                         "/maintenance/notices", "/maintenance/contacts"]),
}

problems = []


def main(only=None):
    OUT.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as p:
        browser = p.chromium.launch()
        # login page on its own (no user)
        page = browser.new_page(viewport={"width": 1440, "height": 900})
        page.goto(BASE + "/login")
        page.wait_for_load_state("networkidle")
        page.screenshot(path=str(OUT / "01_login.png"))
        page.close()

        for role, (username, paths) in ROLES.items():
            if only and role != only:
                continue
            context = browser.new_context(viewport={"width": 1440, "height": 900})
            page = context.new_page()
            page.on("console", lambda m, r=role: m.type == "error" and problems.append(f"[{r}] console: {m.text}"))
            page.on("response", lambda resp, r=role: resp.status >= 400 and "/api/" in resp.url
                    and problems.append(f"[{r}] HTTP {resp.status} {resp.request.method} {resp.url}"))
            page.goto(BASE + "/login")
            page.fill("#username", username)
            page.fill("#password", PASSWORD)
            page.click("button[type=submit], .auth-submit")
            page.wait_for_url(f"**/{role}", timeout=15000)
            for path in paths:
                page.goto(BASE + path)
                page.wait_for_load_state("networkidle")
                page.wait_for_timeout(400)          # let charts finish animating
                name = f"{role}_{path.strip('/').replace('/', '_')}.png"
                page.screenshot(path=str(OUT / name))
                print("saved", name)
            context.close()
        browser.close()

    print("\nProblems found:" if problems else "\nNo console errors or failed API calls.")
    for line in problems:
        print("  " + line)


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else None)
