#!/usr/bin/env sh
set -eu
ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
if [ ! -f "$ROOT_DIR/.env" ]; then
  echo "Copy .env.example to .env and set POSTGRES_PASSWORD first." >&2
  exit 1
fi
set -a
. "$ROOT_DIR/.env"
set +a
: "${POSTGRES_PASSWORD:?Set POSTGRES_PASSWORD}"
export SPRING_PROFILES_ACTIVE=local
export DB_URL="jdbc:postgresql://localhost:${POSTGRES_PORT:-5433}/${POSTGRES_DB:-olarize}"
export DB_USERNAME=olarize_runtime
export DB_PASSWORD="${LOCAL_RUNTIME_PASSWORD:-$POSTGRES_PASSWORD}"
export FLYWAY_URL="$DB_URL"
export FLYWAY_USER="${POSTGRES_USER:-olarize}"
export FLYWAY_PASSWORD="$POSTGRES_PASSWORD"
export SERVER_PORT="${BACKEND_PORT:-8080}"
cd "$ROOT_DIR/backend"
if [ "${1:-}" = "--debug" ]; then
  exec ./mvnw spring-boot:run "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:${DEBUG_PORT:-5005}"
fi
exec ./mvnw spring-boot:run
