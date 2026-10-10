#!/usr/bin/env bash
# River den kortlivade regressionsstacken inklusive volymer.
set -euo pipefail
cd "$(dirname "$0")"
docker compose down -v --remove-orphans
