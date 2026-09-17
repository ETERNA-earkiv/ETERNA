#!/usr/bin/env bash
# Ger en grupp (standard: guests) läsrättighet (READ) på ALLA befintliga AIP:er
# i ETERNA, så att anonyma besökare kan se dem i sökportalen.
#
# Varje AIP hanteras för sig: befintliga rättigheter läses, gruppen läggs
# till i READ, och den uppdaterade listan skickas tillbaka. Inget annat ändras.
# AIP:er som redan har gruppen hoppas över – skriptet kan köras om.
#
# Användning:
#   ETERNA_URL=http://localhost:8080 ETERNA_USER=admin ETERNA_PASSWORD='***' \
#     ./grant-guests-read.sh            # gruppen guests
#   GROUP=Portalgrupp ./grant-guests-read.sh   # annan grupp
#
# Kräver: bash, curl, python3. Kontot måste ha rollen aip.update (admin har det).

set -euo pipefail

ETERNA_URL="${ETERNA_URL:-http://localhost:8080}"
ETERNA_USER="${ETERNA_USER:-admin}"
ETERNA_PASSWORD="${ETERNA_PASSWORD:?Sätt ETERNA_PASSWORD}"
GROUP="${GROUP:-guests}"
PAGE_SIZE=200

api() { # api <method> <path> [json-body]
  local method="$1" path="$2" body="${3:-}"
  if [ -n "$body" ]; then
    curl -sS -u "$ETERNA_USER:$ETERNA_PASSWORD" -X "$method" "$ETERNA_URL$path" \
      -H 'Content-Type: application/json' -d "$body"
  else
    curl -sS -u "$ETERNA_USER:$ETERNA_PASSWORD" -X "$method" "$ETERNA_URL$path"
  fi
}

echo "ETERNA: $ETERNA_URL  användare: $ETERNA_USER  grupp: $GROUP"

# ── 1. Hämta alla AIP:er (alla nivåer) med nuvarande rättigheter ────────────
all_aips="[]"
start=0
while :; do
  page=$(api POST /api/v2/aips/find "{\"filter\":{\"parameters\":[{\"type\":\"AllFilterParameter\"}]},\"sublist\":{\"firstElementIndex\":$start,\"maximumElementCount\":$PAGE_SIZE}}")
  all_aips=$(python3 -c '
import sys, json
acc = json.loads(sys.argv[1]); page = json.loads(sys.argv[2])
acc += [{"id": r["id"], "title": r.get("title"), "permissions": r.get("permissions") or {}} for r in page["results"]]
print(json.dumps({"acc": acc, "total": page["totalCount"]}))
' "$all_aips" "$page")
  total=$(python3 -c 'import sys,json; print(json.loads(sys.argv[1])["total"])' "$all_aips")
  all_aips=$(python3 -c 'import sys,json; print(json.dumps(json.loads(sys.argv[1])["acc"]))' "$all_aips")
  start=$((start + PAGE_SIZE))
  [ "$start" -ge "$total" ] && break
done
count=$(python3 -c 'import sys,json; print(len(json.loads(sys.argv[1])))' "$all_aips")
echo "Hittade $count AIP:er."

# ── 2. Lägg till gruppen i READ per AIP och skicka uppdatering ─────────────
updated=0; skipped=0; failed=0
while IFS= read -r line; do
  id=$(printf '%s' "$line" | python3 -c 'import sys,json; print(json.load(sys.stdin)["id"])')
  title=$(printf '%s' "$line" | python3 -c 'import sys,json; print(json.load(sys.stdin)["title"] or "")')
  request=$(printf '%s' "$line" | python3 -c '
import sys, json
aip = json.load(sys.stdin); group = sys.argv[1]
p = aip["permissions"]
types = ["CREATE", "READ", "UPDATE", "DELETE", "GRANT"]
users  = {t: sorted(set(p.get("users",  {}).get(t) or [])) for t in types}
groups = {t: sorted(set(p.get("groups", {}).get(t) or [])) for t in types}
if group in groups["READ"]:
    print("SKIP"); sys.exit(0)
groups["READ"] = sorted(set(groups["READ"]) | {group})
print(json.dumps({
  "itemsToUpdate": {"@type": "SelectedItemsListRequest", "ids": [aip["id"]]},
  "permissions": {"users": users, "groups": groups},
  "details": f"Sökportalen: läsrättighet för gruppen {group}",
  "recursive": False
}))
' "$GROUP")

  if [ "$request" = "SKIP" ]; then
    skipped=$((skipped + 1)); continue
  fi

  job=$(api PATCH /api/v2/aips/permissions/update "$request")
  job_id=$(printf '%s' "$job" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("id") or "")' 2>/dev/null || true)
  if [ -z "$job_id" ]; then
    echo "  FEL  $id  $title"; echo "       $job" | cut -c1-300; failed=$((failed + 1)); continue
  fi

  # Vänta in jobbet (rättigheter sätts asynkront)
  state=""
  for _ in $(seq 1 60); do
    state=$(api GET "/api/v2/jobs/$job_id" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("state",""))')
    case "$state" in COMPLETED|FAILED_TO_COMPLETE|FAILED_DURING_CREATION|STOPPED) break ;; esac
    sleep 1
  done
  if [ "$state" = "COMPLETED" ]; then
    echo "  OK   $id  $title"; updated=$((updated + 1))
  else
    echo "  FEL  $id  $title  (jobb $job_id: $state)"; failed=$((failed + 1))
  fi
done < <(python3 -c 'import sys,json; [print(json.dumps(a)) for a in json.loads(sys.argv[1])]' "$all_aips")

echo
echo "Klart. Uppdaterade: $updated  Redan klara: $skipped  Fel: $failed"
[ "$failed" -eq 0 ]
