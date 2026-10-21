#!/usr/bin/env bash
# Startar den kortlivade regressionsstacken och väntar tills ETERNA svarar UP.
set -euo pipefail
cd "$(dirname "$0")"

timeout_s="${UP_TIMEOUT_S:-600}"
url="http://localhost:${ETERNA_PORT:-8080}/actuator/health"

show_status() {
  docker compose ps -a --format 'table {{.Service}}\t{{.State}}\t{{.Health}}' >&2
}

if ! docker compose up -d; then
  echo "docker compose up misslyckades. Status:" >&2
  show_status
  exit 1
fi

echo "Väntar på $url (max ${timeout_s}s) ..."
deadline=$((SECONDS + timeout_s))
until curl -fsS --max-time 5 "$url" 2>/dev/null | grep -q '"status":"UP"'; do
  if (( SECONDS >= deadline )); then
    echo "ETERNA blev inte redo inom ${timeout_s}s. Status:" >&2
    show_status
    exit 1
  fi
  sleep 5
done
# /actuator/health blir UP innan RODA initierat klart. Ett /api-anrop dessförinnan
# gör InternalApiAuthFilter permanent AV (se docs/api-probes.md), så vänta på loggraden.
# --since containerns starttid utesluter rader från en tidigare start, och grep läser
# all indata (grep -q kan ge SIGPIPE, exit 141, under pipefail).
started=$(docker inspect -f '{{.State.StartedAt}}' "$(docker compose ps -q eterna)")
until docker compose logs --since "$started" eterna 2>/dev/null | grep -F 'Started RODA' >/dev/null; do
  if (( SECONDS >= deadline )); then
    echo "RODA startade inte inom ${timeout_s}s. Status:" >&2
    show_status
    exit 1
  fi
  sleep 2
done
echo "Stacken är redo på http://localhost:${ETERNA_PORT:-8080}"
