#!/usr/bin/env bash
# Creates the whole database in the correct order (tables -> PL/SQL -> data).
# Run once after the container is healthy. Use reset-db.sh to rebuild from scratch.
set -euo pipefail
cd "$(dirname "$0")"
SCRIPTS=(
  schema/01_core_schema.sql
  schema/02_core_constraints.sql
  schema/03_extension_tables.sql
  functions/04_functions.sql
  procedures/05_procedures.sql
  cursor/06_cursor.sql
  triggers/07_triggers.sql
  seed/08_seed_data.sql
)
for f in "${SCRIPTS[@]}"; do
  echo ">>> $f"
  ./run-sql.sh "$f" > /tmp/shms-last-run.log 2>&1 || { cat /tmp/shms-last-run.log; echo "FAILED: $f"; exit 1; }
  if grep -q "compilation errors" /tmp/shms-last-run.log; then cat /tmp/shms-last-run.log; echo "PL/SQL COMPILE ERROR in $f"; exit 1; fi
done
echo ">>> verifying"
./run-sql.sh verify.sql
