#!/usr/bin/env sh
# Shared branch policy for pre-commit and commit-msg.
set -eu
branch=$(git symbolic-ref --quiet --short HEAD || :)
if ! printf '%s\n' "$branch" | LC_ALL=C grep -Eq '^(feature|fix)/OLARIZE-[1-9][0-9]*$'; then
    printf '%s\n' 'Olarize: commits require a branch named feature/OLARIZE-<ticket number> or fix/OLARIZE-<ticket number>, for example feature/OLARIZE-18.' >&2
    exit 1
fi
printf '%s\n' "${branch#*/}"
