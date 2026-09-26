#!/usr/bin/env sh
set -eu
ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT_DIR"
# Inspect the effective value without replacing an existing hook manager.
CURRENT_HOOKS=$(git config --get core.hooksPath || :)
if [ -n "$CURRENT_HOOKS" ] && [ "$CURRENT_HOOKS" != ".githooks" ]; then
    printf '%s\n' 'Olarize: an existing hooksPath is configured; integrate it explicitly before installation.' >&2
    exit 1
fi
if [ -z "$CURRENT_HOOKS" ] && [ -f .git/hooks/pre-commit ]; then
    printf '%s\n' 'Olarize: an existing pre-commit hook exists; integrate it explicitly before installation.' >&2
    exit 1
fi
chmod +x .githooks/pre-commit
git config --local core.hooksPath .githooks
printf '%s\n' 'Olarize: project-local Git hooks enabled.'
