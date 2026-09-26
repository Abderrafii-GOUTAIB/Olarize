#!/usr/bin/env sh
set -eu
WORKBENCH_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
export PYTHONDONTWRITEBYTECODE=1
exec python3 "$WORKBENCH_DIR/service/workbench.py" "$@"
