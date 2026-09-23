#!/usr/bin/env sh
set -eu
ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
# Capture the selected context before DOCKER_HOST makes the CLI report default.
DOCKER_CONTEXT_NAME=$(docker context show)
# Testcontainers may not automatically discover a nondefault Docker context.
if [ -z "${DOCKER_HOST:-}" ]; then
  DOCKER_HOST=$(docker context inspect --format '{{.Endpoints.docker.Host}}')
  export DOCKER_HOST
fi
if { [ "$DOCKER_CONTEXT_NAME" = "rancher-desktop" ] || [ "$DOCKER_HOST" = "unix://$HOME/.rd/docker.sock" ]; } && [ -z "${TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE:-}" ]; then
  export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
fi
cd "$ROOT_DIR/backend"
exec ./mvnw -B clean verify "$@"
