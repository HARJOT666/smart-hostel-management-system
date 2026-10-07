#!/usr/bin/env bash
# DANGER: deletes all application tables and data, then rebuilds everything.
set -euo pipefail
cd "$(dirname "$0")"
./run-sql.sh drop-all.sql
./run-all.sh
