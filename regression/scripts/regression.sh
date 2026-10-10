#!/usr/bin/env bash
# Startar den kortlivade stacken, kör sviten och river stacken, även om testerna fallerar.
# Argument skickas vidare till playwright test, t.ex. --project=smoke.
set -uo pipefail
cd "$(dirname "$0")/.."

export ETERNA_URL="http://localhost:${ETERNA_PORT:-8080}"

save_logs() {
  mkdir -p test-results
  docker compose -f env/docker-compose.yaml logs --no-color > test-results/compose.log 2>&1 || true
  echo "Containerloggar sparade i regression/test-results/compose.log" >&2
}

# Stackens egna inloggningsuppgifter, så att en .env (t.ex. för stage) aldrig används mot den.
export ETERNA_USER=admin ETERNA_PASSWORD=eterna

# Riv alltid stacken vid avslut, även vid Ctrl-C eller avbruten CI-körning.
status=1
teardown() {
  if [ "$status" -ne 0 ]; then
    save_logs
  fi
  ./env/down.sh
}
trap teardown EXIT
child=
stop_child() {
  if [ -n "$child" ]; then
    pkill -TERM -P "$child" 2>/dev/null
    kill -TERM "$child" 2>/dev/null
    wait "$child" 2>/dev/null
  fi
}
trap 'status=130; stop_child; exit 130' INT
trap 'status=143; stop_child; exit 143' TERM

if ./env/up.sh; then
  # I bakgrunden + wait, så att bash kör trap direkt i stället för efter att testerna är klara.
  bunx playwright test "$@" &
  child=$!
  wait "$child"
  status=$?
  child=
fi
exit "$status"
