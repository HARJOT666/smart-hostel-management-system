#!/usr/bin/env bash
# Demonstrates and tests the two Redis features against the RUNNING backend.
#   1. Cache:      miss -> hit -> write evicts -> fresh read
#   2. Rate limit: 5 failed logins -> HTTP 429 -> key expires -> login allowed
#
# Usage: tools/redis-demo-test.sh [path-to-backend-log]
#   The optional log path lets the script count "CACHE MISS" lines (Oracle reads).
#   Set SKIP_WAIT=1 to skip the 5-minute wait at the end.
set -uo pipefail
API=${API:-http://localhost:8080/api}
LOG=${1:-}
redis() { docker exec shms-redis redis-cli "$@"; }
misses() { if [ -n "$LOG" ]; then grep -c "CACHE MISS: loading announcements (hostel 1)" "$LOG"; else echo "n/a"; fi; }
json() { python3 -c "import sys,json; d=json.load(sys.stdin); print($1)"; }
login() { curl -s -o /tmp/shms-login.json -w "%{http_code}" -X POST "$API/auth/login" \
            -H 'Content-Type: application/json' -d "{\"username\":\"$1\",\"password\":\"$2\"}"; }

echo "Smart Hostel - Redis test run at $(date '+%Y-%m-%d %H:%M:%S')"
login admin@shms.local Hostel@123 >/dev/null
TOKEN=$(json "d['token']" < /tmp/shms-login.json)
AUTH="Authorization: Bearer $TOKEN"

echo
echo "================ PART 1: CACHE (announcements of hostel 1) ================"
redis DEL "shms:announcements::1" >/dev/null
echo "Start: key shms:announcements::1 exists? $(redis EXISTS shms:announcements::1)   Oracle reads so far: $(misses)"

N=$(curl -s -H "$AUTH" "$API/announcements?hostelId=1" | json "len(d)")
echo "1) GET #1 -> $N announcements | key exists? $(redis EXISTS shms:announcements::1) TTL=$(redis TTL shms:announcements::1)s | Oracle reads: $(misses)  (cache MISS -> read from Oracle, stored in Redis)"

N=$(curl -s -H "$AUTH" "$API/announcements?hostelId=1" | json "len(d)")
echo "2) GET #2 -> $N announcements | Oracle reads: $(misses)  (unchanged -> served from Redis cache)"

TITLE="Redis test notice $(date +%H%M%S)"
NEW_ID=$(curl -s -X POST -H "$AUTH" -H 'Content-Type: application/json' "$API/announcements" \
  -d "{\"title\":\"$TITLE\",\"body\":\"Created by tools/redis-demo-test.sh\",\"category\":\"General\",\"hostelId\":1,\"pinned\":false}" | json "d['id']")
echo "3) POST new announcement id=$NEW_ID | key exists now? $(redis EXISTS shms:announcements::1)  (@CacheEvict removed the stale list)"

RESP=$(curl -s -H "$AUTH" "$API/announcements?hostelId=1")
N=$(echo "$RESP" | json "len(d)"); FOUND=$(echo "$RESP" | json "any(a['title']=='$TITLE' for a in d)")
echo "4) GET #3 -> $N announcements, new notice present: $FOUND | Oracle reads: $(misses)  (fresh data from Oracle, cached again)"

curl -s -o /dev/null -w "" -X DELETE -H "$AUTH" "$API/announcements/$NEW_ID"
echo "5) DELETE test announcement $NEW_ID | key exists? $(redis EXISTS shms:announcements::1)  (evicted again)"

echo
echo "================ PART 2: LOGIN RATE LIMIT (5 failures / 5 minutes / IP) ================"
for k in $(redis --scan --pattern 'rate:login:fail:*'); do redis DEL "$k" >/dev/null; done
for i in 1 2 3 4 5; do
  CODE=$(login warden@shms.local wrong-password-$i)
  KEY=$(redis --scan --pattern 'rate:login:fail:*' | head -1)
  echo "Failed attempt $i -> HTTP $CODE | Redis $KEY = $(redis GET "$KEY") (TTL $(redis TTL "$KEY")s)"
done
CODE=$(curl -s -o /tmp/shms-429.json -D /tmp/shms-429.h -w "%{http_code}" -X POST "$API/auth/login" \
       -H 'Content-Type: application/json' -d '{"username":"warden@shms.local","password":"Hostel@123"}')
echo "6th attempt (CORRECT password) -> HTTP $CODE | Retry-After: $(grep -i retry-after /tmp/shms-429.h | tr -d '\r' | cut -d' ' -f2)"
echo "   body: $(cat /tmp/shms-429.json)"

if [ "${SKIP_WAIT:-0}" = "1" ]; then echo "(SKIP_WAIT=1: not waiting for expiry)"; exit 0; fi
TTL=$(redis TTL "$KEY")
echo "Waiting $((TTL + 2)) seconds for the Redis key to expire..."
sleep $((TTL + 2))
echo "Key exists after expiry? $(redis EXISTS "$KEY")"
CODE=$(login warden@shms.local Hostel@123)
echo "Login after expiry (correct password) -> HTTP $CODE"
