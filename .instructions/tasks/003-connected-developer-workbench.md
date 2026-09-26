# Task 003 — Connected developer workbench

Status: implemented local service and connected dashboard; 29 service tests and dashboard interaction checks passed; live-provider validation pending permission and environment readiness.

## User outcome

Operate the development workflow from the dashboard, from module discovery and story approval through implementation, tests, independent review, human code inspection and draft PR creation. This is development tooling, separate from Olarize's HR runtime.

## Scope and contract

The agreed specification is `developer-workbench/SPECIFICATION.md`. Follow its branch policy, approval gates, shared run tracking, project folder boundary, recovery behavior and acceptance scenarios. Preserve the current manual planner until connected stages work. Do not treat historical/manual dashboard state as fresh execution evidence.

## Implementation order

Start WB-001: verify supported Codex execution and connector access, actual Git branches/remotes, required committed instructions and project-boundary compatibility. Record the chosen adapter and observed limitations. Then deliver WB-002 through WB-007 in order. Do not select a protocol based only on assumptions, publish Jira issues, push branches or create PRs while merely validating feasibility.

## Verification

Use the specification's milestone acceptance scenarios; report actual checks per milestone. Story implementation uses the existing backend verification and review workflow. A simulated provider is insufficient to claim live connectivity. Verified on 2026-09-24: 29 service tests passed using real SQLite, Git and local HTTP with mocked providers; dashboard interaction checks, JavaScript/shell syntax and whitespace checks passed. No visual browser check or successful live Codex/Jira/GitHub end-to-end run is claimed. Independent review identified five findings; fixes and regression tests are included. The independent re-review could not complete because the reviewer hit an account usage limit.

## Completion notes

The user-approved workflow is captured in a durable project specification. Existing assets are the manual dashboard, skills, reviewer configuration, hook and backend CI. Connected service, provider adapters, durable execution state, live events, diff review and guarded PR publication are now implemented. Live Codex/Jira/GitHub end-to-end execution remains unverified. See developer-workbench/README.md for the launch flags, prerequisites and limits.
