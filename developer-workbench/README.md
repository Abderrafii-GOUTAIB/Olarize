# Olarize connected development studio

Local development tooling, separate from the HR backend/frontend and production deployment. The connected dashboard uses the Python-standard-library service in `service/workbench.py`, SQLite state, Codex CLI executions and a GitHub REST adapter. The original manual planner remains available at `/planner` and `index.html`.

## Launch

From the repository root:

```sh
./developer-workbench/start.sh
```

Open the private launch URL printed by the service. It uses port 4179 and a per-service local session token. It is a different page from the old `file://.../index.html` planner. The connected page requires the service; serving static files alone cannot execute runs. Do not share the session URL or token. State and artifacts are ignored under `developer-workbench/.state/`; worktrees go under `.worktrees/`.

The default service allows browsing, drafting edits, approvals and reviewing stored state, but blocks live Codex execution. The user's strict folder boundary requires an explicit exception for the installed CLI's existing account login. Only after the user permits that exception, start with:

```sh
./developer-workbench/start.sh --allow-existing-codex-login
```

This runner ignores user config, uses ephemeral Codex sessions, redirects SQLite state and logs into the project, and leaves existing authentication to the CLI. It does not change CODEX_HOME or copy credentials. The installed runtime may still use its standard auth and support files; these cannot be represented as wholly project-contained. Live provider authentication must be verified separately; no successful live run is claimed by installing the service.

## Workflow

1. Generate stories for a module. The story skill runs through Codex and returns a draft catalogue and full module document. Results are stored in SQLite and per-run artifacts; the full module document is returned in run data. Existing story IDs are preserved. Employee records are initially seeded from the documented requirements, including approval hashes and Jira links; those are source snapshots, not live Jira status.
2. Read/edit each draft and approve its exact version. Publish approved stories through the project-local Atlassian MCP via a separate Codex execution. It searches for duplicates and verifies backlog placement. Authentication or scope questions appear as waiting-for-input states. Published story edits are blocked until an explicit synchronization feature exists.
3. Select one approved, Jira-linked story. Specify the agreed backend scope and dependency evidence. Start a feature/fix/chore run. The service requires a clean source checkout and fetches `origin/develop`. Required instructions/hooks must be committed there. It never commits existing setup changes for you or silently renames branches.
4. Implementation runs in an isolated project-contained Git worktree. The service runs backend verification, whitespace checking, Compose validation and the Docker build. A new read-only Codex session reviews with `olarize-review`; the service permits up to two automatic fix cycles before asking for direction.
5. Load the generated diff, inspect actual checks/findings and request corrections. The publication button approves that exact snapshot. Source changes invalidate it. The service freezes and verifies the commit tree before pushing a specific commit SHA; it does not push an unverified moving HEAD.
6. PR creation uses your repository's actual GitHub origin, a draft PR into `develop`, and GITHUB_TOKEN supplied to the server process by your credential manager. Tokens are not accepted by the dashboard or written to config. Git pushing uses the installed Git authentication separately. Retried publication reconciles an existing branch/PR. The service never merges or deploys.
7. Refresh GitHub checks to observe statuses for the current PR head. Required branch protection and merge decisions remain GitHub configuration/user responsibilities. The UI does not infer success from an empty list of checks or claim that PR creation means Done in Jira.

## Shared Codex / terminal entry point

Both entry points use the same server and transactionally locked run database:

```sh
python3 developer-workbench/control.py status
python3 developer-workbench/control.py stories
python3 developer-workbench/control.py runs
```

Start a run by POSTing the selected story ID, its current version, prefix and agreed scope to `runs`, using `--body` with a JSON object. Run actions are `runs/<id>/answer`, `/retry`, `/cancel`, `/publish` and `/ci`. Read `/diff` before publication and supply its exact `revision`. API mutations require the local session token; the control client reads it only from the project-local state directory. No second engine is launched.

## Verification and operating limits

```sh
PYTHONDONTWRITEBYTECODE=1 python3 developer-workbench/service/test_workbench.py
node --check developer-workbench/connected.js
node developer-workbench/test_connected.js
```

Tests use isolated project-local fixtures, real SQLite/Git/local HTTP, and mocked provider calls. They do not create real Jira issues, run paid model inference, push branches or publish GitHub PRs. GitHub Actions runs the workbench tests separately from backend CI; push triggers target `master` and `develop`.

The first version serializes agent work. Restarted queued/running runs become interrupted and require explicit retry; retained artifacts support diagnosis. Cancellation terminates the active process group, retains work and cannot undo external objects already created. Do not run a second orchestrator against the same state. Do not manually edit an active worktree during implementation/review/publication. Native tool permission requests in noninteractive Codex fail visibly and require intervention; the service does not bypass the sandbox.

A source checkout with uncommitted work, a missing remote `develop`, missing CLI/MCP authentication, unavailable Docker or missing GitHub credentials is a visible blocker. Deployment, hotfix/release execution, automatic Jira Done, published-story editing/sync, GitHub branch-protection administration and automatic worktree cleanup are not implemented. The service does not attach a PR to a desktop task automatically; it returns its actual URL so a Codex task can attach it using the app's artifact tool.

Browser visual validation and live provider execution must be reported separately from service test results. Do not promote manual planner localStorage notes into verified run evidence. The existing planner and its static story refresh script remain unchanged in purpose.

See [SPECIFICATION.md](SPECIFICATION.md) for the agreed target and milestones.

Verified 2026-09-24: 29 service tests and dashboard interaction checks passed. Initial independent review findings were fixed with regression tests; independent re-review was unavailable due to an account usage limit. Successful live-provider execution and visual browser validation remain pending.

## Understand the moving parts

Open [the interactive instruction map](instruction-guide.html) as a local file, or read [the complete written explanation](../docs/understanding-the-workflow.md). The guide explains every instruction/skill, the reviewer role, hook, pipeline, and the distinction between the HR product and development tools. It requires no login or running service.
