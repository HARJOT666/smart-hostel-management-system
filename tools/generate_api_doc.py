"""Builds docs/api.md by reading the @RequestMapping/@GetMapping/... and @PreAuthorize annotations in the controllers."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CTRL = ROOT / "backend/src/main/java/com/smarthostel/controller"
rows = []
for f in sorted(CTRL.glob("*.java")):
    src = f.read_text()
    base = re.search(r'@RequestMapping\("([^"]+)"\)', src).group(1)
    class_auth = re.search(r'@PreAuthorize\("([^"]+)"\)\s*public class', src)
    class_roles = class_auth.group(1) if class_auth else "any logged-in user"
    # each method block: annotations then "public ... name("
    for m in re.finditer(r'((?:\s*(?:/\*\*.*?\*/|@[^\n]+)\n)+)\s*public [^(]+ (\w+)\(', src, re.S):
        annotations, name = m.group(1), m.group(2)
        verb = re.search(r'@(Get|Post|Put|Delete)Mapping(?:\((?:value = )?"?([^",)]*)"?)?', annotations)
        if not verb:
            continue
        path = base + (verb.group(2) or "")
        auth = re.search(r'@PreAuthorize\("([^"]+)"\)', annotations)
        roles = auth.group(1) if auth else class_roles
        roles = roles.replace("hasAnyRole(", "").replace("hasRole(", "").replace(")", "").replace("'", "")
        if path in ("/api/auth/login", "/api/auth/register"):
            roles = "public"
        doc = re.search(r'/\*\*\s*(.*?)\s*\*/', annotations, re.S)
        rows.append((f.stem.replace("Controller", ""), verb.group(1).upper(), path, roles, doc.group(1).replace("\n", " ") if doc else ""))

out = ["# REST API", "", "Generated from the controller source by `tools/generate_api_doc.py`.", "",
       "Base URL `http://localhost:8080`. Send `Authorization: Bearer <JWT>` except for public endpoints.",
       "Errors are JSON: `{status, error, message, timestamp}` — 400 validation/business rule (incl. ORA-20xxx), "
       "401 not logged in, 403 wrong role, 404 not found, 409 constraint violation, 429 too many failed logins.", "",
       "| Module | Method | Path | Roles | Notes |", "|---|---|---|---|---|"]
out += [f"| {a} | {b} | `{c}` | {d} | {e} |" for a, b, c, d, e in rows]
(ROOT / "docs/api.md").write_text("\n".join(out) + "\n")
print(len(rows), "endpoints")
