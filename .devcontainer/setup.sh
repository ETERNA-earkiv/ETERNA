#!/usr/bin/env bash
set -euo pipefail

echo "==> Setting up ETERNA dev environment..."

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SOURCE_COMPOSE_FILE="$REPO_ROOT/deploys/standalone/docker-compose-dev.yaml"
GENERATED_COMPOSE_FILE="$SCRIPT_DIR/docker-compose-devcontainer.yaml"

# ── Maven settings.xml with GitHub PAT ────────────────────────────────────────
mkdir -p "$HOME/.m2"

if [ -n "${GITHUB_PAT:-}" ]; then
  cat > "$HOME/.m2/settings.xml" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0
                              https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>GITHUB_PAT</username>
      <password>${GITHUB_PAT}</password>
    </server>
  </servers>
</settings>
EOF
  echo "    Container-local Maven settings.xml written."
else
  echo "    WARNING: GITHUB_PAT not set — Maven will fail to resolve GitHub Packages."
  echo "    Set it on your host: export GITHUB_PAT=ghp_... (then rebuild container)"
fi

# ── RODA data directories ──────────────────────────────────────────────────────
mkdir -p "$HOME/.roda/data/storage" "$HOME/.roda/data/staging-storage"
echo "    RODA data dirs ready."

# ── Docker socket permissions ──────────────────────────────────────────────────
# Align container docker group GID with the actual socket GID from the host
DOCKER_SOCK_GID=$(stat -c '%g' /var/run/docker.sock 2>/dev/null || echo "")
if [ -n "$DOCKER_SOCK_GID" ] && [ "$DOCKER_SOCK_GID" != "0" ]; then
  CURRENT_GID=$(getent group docker | cut -d: -f3 2>/dev/null || echo "")
  if [ "$CURRENT_GID" != "$DOCKER_SOCK_GID" ]; then
    sudo groupmod -g "$DOCKER_SOCK_GID" docker 2>/dev/null || true
  fi
fi

# ── Devcontainer-specific Compose file ────────────────────────────────────────
if [ -n "${HOST_HOME:-}" ]; then
  sed \
    -e "s|\${HOME}|$HOST_HOME|g" \
    -e "s|\$HOME|$HOST_HOME|g" \
    "$SOURCE_COMPOSE_FILE" > "$GENERATED_COMPOSE_FILE"
  echo "    Generated $GENERATED_COMPOSE_FILE with host paths."
else
  echo "    WARNING: HOST_HOME not set — using the raw compose file may break bind mounts."
fi

echo ""
echo "==> Setup complete. Next steps:"
echo ""
echo "  1. Start infrastructure:"
if [ -n "${HOST_HOME:-}" ]; then
  echo "     docker compose -f .devcontainer/docker-compose-devcontainer.yaml up -d"
else
  echo "     docker compose -f deploys/standalone/docker-compose-dev.yaml up -d"
fi
echo ""
echo "  2. Build ETERNA core:"
echo "     mvn install -Pcore -DskipTests"
echo ""
echo "  3. Run the app:"
echo "     mvn -pl roda-ui/roda-wui -am spring-boot:run -Pdebug-main"
echo "     → http://localhost:8080  (admin / eterna)"
echo ""
