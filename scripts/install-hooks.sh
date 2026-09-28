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
if [ -z "$CURRENT_HOOKS" ]; then
    for hook in pre-commit commit-msg post-commit; do
        if [ -f "$(git rev-parse --git-path "hooks/$hook")" ]; then
            printf 'Olarize: an existing %s hook exists; integrate it explicitly before installation.\n' "$hook" >&2
            exit 1
        fi
    done
fi
chmod +x .githooks/pre-commit .githooks/commit-msg .githooks/post-commit
git config --local core.hooksPath .githooks
printf '%s\n' 'Olarize: project-local Git hooks enabled.'
