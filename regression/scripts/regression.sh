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

if ! ./env/up.sh; then
  save_logs
  ./env/down.sh
  exit 1
fi

bunx playwright test "$@"
status=$?

if [ "$status" -ne 0 ]; then
  save_logs
fi
./env/down.sh
exit "$status"
