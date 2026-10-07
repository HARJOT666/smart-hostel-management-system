#!/usr/bin/env bash
# Run one SQL file inside the Oracle container as the application user.
# Usage: ./run-sql.sh schema/01_core_schema.sql
set -euo pipefail
cd "$(dirname "$0")"
source .env
FILE="$1"
docker exec -i shms-oracle bash -c "cd /opt/shms-sql/$(dirname "$FILE") && sqlplus -s ${DB_USERNAME}/${DB_PASSWORD}@localhost/FREEPDB1" <<SQL
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK
SET ECHO OFF FEEDBACK ON SERVEROUTPUT ON SIZE UNLIMITED LINESIZE 200 PAGESIZE 100 DEFINE OFF
@$(basename "$FILE")
EXIT
SQL
